package won.matcher.service.common.service.sparql;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

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
    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected String sparqlEndpoint;
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
     * of the new model.
     *
     * @param graph named graph to be updated
     * @param model model that holds triples to set
     */
    public String createUpdateNamedGraphQuery(String graph, Model model) {
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, model, Lang.NTRIPLES);
        String query = "\nCLEAR GRAPH ?g;\n" + "\nINSERT DATA { GRAPH ?g { " + sw + "}};\n";
        ParameterizedSparqlString pps = new ParameterizedSparqlString();
        pps.setCommandText(query);
        pps.setIri("g", graph);
        return pps.toString();
    }

    /**
     * Update a dataset of names graphs first deleting them and afterwards inserting
     * the triples of the new models.
     *
     * @param ds
     */
    public void updateNamedGraphsOfDataset(Dataset ds) {
        String query = "";
        Iterator<String> graphNames = ds.listNames();
        while (graphNames.hasNext()) {
            logger.debug("Save dataset");
            String graphName = graphNames.next();
            Model model = ds.getNamedModel(graphName);
            query += createUpdateNamedGraphQuery(graphName, model);
            // Update can also be done with accessor - use put/add?
            // accessor.add(graphName, model);
        }
        if (query != "") {
            executeUpdateQuery(query);
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
            logger.debug("Update SPARQL Endpoint: {}", sparqlEndpoint);
            logger.debug("Execute query: {}", updateQuery);
            UpdateRequest query = UpdateFactory.create(updateQuery);
            UpdateProcessRemote riStore = (UpdateProcessRemote) UpdateExecutionFactory.createRemote(query,
                            sparqlEndpoint);
            riStore.execute();
        } catch (QueryParseException e) {
            logger.warn("Error parsing update query: " + updateQuery, e);
        }
    }
}
