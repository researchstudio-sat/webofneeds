package won.matcher.service.common.service.sparql;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.modify.UpdateProcessRemote;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.util.RdfUtils;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to access of Sparql enpoint database to save or query linked data.
 * User: hfriedrich Date: 15.04.2015
 */
@Component
public class SparqlService {
    // ran into a stack overflow at about 1000 triples with the same subject. The
    // current value of this constant chosen lower, with limited amount of thought
    // having gone into it.
    protected static final int MAX_INSERT_TRIPLES_COUNT = 250;
    // ran into a stack overflow in RematchSparqlService due to too many update
    // where statements in one request. the recursion depth was 1000. Using the same
    // limit as for triples, for now, although that would not work for a combination
    // of both.
    protected static final int MAX_UPDATES_PER_REQUEST = 250;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected String sparqlEndpoint;
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    // protected DatasetAccessor accessor;

    public SparqlService(@Autowired String sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
        // accessor = DatasetAccessorFactory.createHTTP(sparqlEndpoint);
    }

    public static Dataset deserializeDataset(String serializedResource, Lang format) throws IOException {
        InputStream is = new ByteArrayInputStream(serializedResource.getBytes(StandardCharsets.UTF_8));
        Dataset ds = RdfUtils.toDataset(is, new RDFFormat(format));
        is.close();
        return ds;
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    /**
     * Update named graph by first deleting it and afterwards inserting the triples
     * of the new model. If there are too many triples in the model, split them up
     * in multiple insert statements. We need to perfom input chunking because the
     * jena sparql parser cannot handle arbitrarily long input.
     *
     * @param graph named graph to be updated
     * @param model model that holds triples to set
     */
    public Optional<String> createUpdateNamedGraphQueryChunked(String graph, Model model) {
        StringBuilder query = new StringBuilder();
        query.append("\nCLEAR GRAPH ?g;\n");
        // write the model into a pipedInputStream in a separate thread,
        // read from the connected pipedOutputStream and write triples into the
        // query in chunks
        List<String> chunks = null;
        try {
            final PipedOutputStream pout = new PipedOutputStream();
            final PipedInputStream pin = new PipedInputStream(pout);
            executorService.execute(() -> {
                try {
                    RDFDataMgr.write(pout, model, Lang.NTRIPLES);
                    pout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            chunks = readInChunks(pin, MAX_INSERT_TRIPLES_COUNT);
            pin.close();
        } catch (IOException e) {
            logger.warn("Error making chunks of ntriples", e);
        }
        if (chunks == null) {
            return Optional.empty();
        }
        for (String chunk : chunks) {
            query
                            .append("\nINSERT DATA { GRAPH ?g { ")
                            .append(chunk)
                            .append("}};\n");
        }
        ParameterizedSparqlString pps = new ParameterizedSparqlString();
        pps.setCommandText(query.toString());
        pps.setIri("g", graph);
        return Optional.of(pps.toString());
    }

    /**
     * Update a dataset of names graphs first deleting them and afterwards inserting
     * the triples of the new models.
     *
     * @param ds
     */
    public void updateNamedGraphsOfDataset(Dataset ds) {
        StringBuilder query = new StringBuilder();
        Iterator<String> graphNames = ds.listNames();
        while (graphNames.hasNext()) {
            logger.debug("Save dataset");
            String graphName = graphNames.next();
            Model model = ds.getNamedModel(graphName);
            createUpdateNamedGraphQueryChunked(graphName, model).map(query::append);
        }
        if (query.length() > 0) {
            executeUpdateQuery(query.toString());
        }
    }

    public Model retrieveModel(String graphName) {
        String queryTemplate = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } . }";
        ParameterizedSparqlString pps = new ParameterizedSparqlString();
        pps.setCommandText(queryTemplate);
        pps.setIri("g", graphName);
        Query query = QueryFactory.create(pps.toString());
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
            Model model = qexec.execConstruct();
            return model;
        }
    }

    public Dataset retrieveDataset(String graphName) {
        Dataset ds = TDBFactory.createDataset();
        ds.asDatasetGraph().getContext().set(TDB.symUnionDefaultGraph, new NodeValueBoolean(true));
        Model model = retrieveModel(graphName);
        ds.addNamedModel(graphName, model);
        return ds;
    }

    public Dataset retrieveAtomDataset(String atomUri) {
        String queryString = "prefix won: <https://w3id.org/won/core#> select distinct ?g where { "
                        + "GRAPH ?g { ?uri a won:Atom. ?a ?b ?c. } }";
        ParameterizedSparqlString pps = new ParameterizedSparqlString();
        pps.setCommandText(queryString);
        pps.setIri("uri", atomUri);
        Query query = QueryFactory.create(pps.toString());
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
            ResultSet results = qexec.execSelect();
            Dataset ds = DatasetFactory.createGeneral();
            while (results.hasNext()) {
                QuerySolution qs = results.next();
                String graphUri = qs.getResource("g").getURI();
                Model model = retrieveModel(graphUri);
                ds.addNamedModel(graphUri, model);
            }
            return ds;
        }
    }

    public void deleteAtom(String atomUri) {
        Set<String> graphsToDelete = getGetAtomAndMessagesNamedGraphUris(atomUri);
        if (graphsToDelete.isEmpty()) {
            // nothing found, nothing to do
            return;
        }
        deleteNamedGraphs(graphsToDelete);
    }

    public void deleteAtomMetadata(String atomUri) {
        executeUpdateQuery(getDeleteAtomMetadataUpdate(atomUri));
    }

    public void deleteNamedGraphs(Set<String> graphUris) {
        executeUpdateQuery(getDeleteGraphsUpdate(graphUris));
    }

    private String getDeleteGraphsUpdate(Set<String> graphUris) {
        StringBuilder builder = new StringBuilder();
        for (String graphUri : graphUris) {
            builder
                            .append("DELETE WHERE {\n")
                            .append("  GRAPH <").append(graphUri).append("> {\n")
                            .append("    ?s ?p ?o .\n")
                            .append("  }\n")
                            .append("};\n");
        }
        return builder.toString();
    }

    /**
     * Execute a SPARQL Update query.
     *
     * @param updateQuery
     */
    public void executeUpdateQuery(String updateQuery) {
        try {
            parseUpdateQuery(updateQuery).ifPresent(r -> executeUpdate(r));
        } catch (Exception e) {
            logger.warn("Error executing update: " + updateQuery, e);
        }
    }

    /**
     * Parse a SPARQL Update query and generate an UpdateRequest
     *
     * @param updateQuery
     */
    public Optional<UpdateRequest> parseUpdateQuery(String updateQuery) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Parsing update: {}", updateQuery);
            }
            return Optional.of(UpdateFactory.create(updateQuery));
        } catch (QueryParseException e) {
            logger.warn("Error parsing update query: " + updateQuery, e);
        }
        return Optional.empty();
    }

    public void executeUpdate(UpdateRequest updateRequest) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Update SPARQL Endpoint: {}", sparqlEndpoint);
                logger.debug("number of updates: {}", updateRequest.getOperations().size());
            }
            UpdateProcessRemote riStore = (UpdateProcessRemote) UpdateExecutionFactory.createRemote(updateRequest,
                            sparqlEndpoint);
            riStore.execute();
        } catch (Exception e) {
            logger.warn("Error sending update request: {} ", updateRequest, e);
        }
    }

    private Set<String> getGetAtomAndMessagesNamedGraphUris(String atomUri) {
        StringBuilder builder = new StringBuilder();
        builder
                        .append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n")
                        .append("PREFIX won: <https://w3id.org/won/core#>\n")
                        .append("SELECT DISTINCT ?graph\n")
                        .append("WHERE\n")
                        .append("{\n")
                        .append("  {\n")
                        .append("           ?atomUri ?p ?g ;\n")
                        .append("                    a won:Atom .\n")
                        .append("           GRAPH ?g {\n")
                        .append("             ?x ?y ?z\n")
                        .append("           }\n")
                        .append("           BIND (?g as ?graph)\n")
                        .append("  } UNION {\n")
                        .append("           ?atomUri ?p ?g ;\n")
                        .append("                    a won:Atom .\n")
                        .append("           GRAPH ?g {\n")
                        .append("             ?atomUri won:messageContainer/rdfs:member ?m\n")
                        .append("           }\n")
                        .append("           BIND (?m as ?graph)\n")
                        .append("  }\n")
                        .append("}\n");
        ParameterizedSparqlString pss = new ParameterizedSparqlString(builder.toString());
        pss.setIri("atomUri", atomUri);
        Query query = QueryFactory.create(pss.toString());
        Set<String> ret = new HashSet<>();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                RDFNode graphUri = qs.get("graph");
                if (graphUri.isURIResource()) {
                    ret.add(graphUri.asResource().getURI());
                }
            }
        }
        return ret;
    }

    private String getDeleteAtomMetadataUpdate(String atomUri) {
        // insert a new entry,
        StringBuilder builder = new StringBuilder();
        // delete rematch data
        builder
                        .append(" DELETE WHERE {  \n")
                        .append("  graph won:rematchMetadata {  \n")
                        .append("    ?atomUri ?p ?o . \n")
                        .append("  } \n")
                        .append(" }; \n")
                        // delete crawling data
                        .append(" DELETE WHERE {  \n")
                        .append("  graph won:crawlMetadata {  \n")
                        .append("    ?atomUri ?p ?o . \n")
                        .append("  } \n")
                        .append(" }; \n");
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(builder.toString());
        pss.setNsPrefix("won", "https://w3id.org/won/core#");
        pss.setIri("atomUri", atomUri);
        long now = System.currentTimeMillis();
        pss.setLiteral("matchAttemptDate", now);
        pss.setLiteral("referenceDate", now);
        return pss.toString();
    }

    private List<String> readInChunks(final PipedInputStream pr, final int chunkSize) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(pr));
        String line = null;
        List<String> chunks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int linecnt = 0;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
            linecnt++;
            if (linecnt % chunkSize == 0) {
                chunks.add(sb.toString());
                sb = new StringBuilder();
            }
        }
        if (sb.length() > 0) {
            chunks.add(sb.toString());
        }
        br.close();
        return chunks;
    }
}
