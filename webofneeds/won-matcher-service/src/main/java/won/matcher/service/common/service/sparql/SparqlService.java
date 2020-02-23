package won.matcher.service.common.service.sparql;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import won.protocol.util.RdfUtils;

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

    public static Dataset deserializeDataset(String serializedResource, Lang format) throws IOException {
        InputStream is = new ByteArrayInputStream(serializedResource.getBytes(StandardCharsets.UTF_8));
        Dataset ds = RdfUtils.toDataset(is, new RDFFormat(format));
        is.close();
        return ds;
    }

    @Autowired
    public SparqlService(@Value("${uri.sparql.endpoint}") String sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
        // accessor = DatasetAccessorFactory.createHTTP(sparqlEndpoint);
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
        if (chunks == null)
            return Optional.empty();
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
        DatasetGraph dsg = TDBFactory.createDatasetGraph();
        dsg.getContext().set(TDB.symUnionDefaultGraph, new NodeValueBoolean(true));
        Dataset ds = DatasetFactory.create(dsg);
        Model model = retrieveModel(graphName);
        ds.addNamedModel(graphName, model);
        return ds;
    }

    public Dataset retrieveAtomDataset(String uri) {
        String queryString = "prefix won: <https://w3id.org/won/core#> select distinct ?g where { "
                        + "GRAPH ?g { ?uri a won:Atom. ?a ?b ?c. } }";
        ParameterizedSparqlString pps = new ParameterizedSparqlString();
        pps.setCommandText(queryString);
        pps.setIri("uri", uri);
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
