package won.utils.crawl.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.tdb.TDB;
import org.apache.jena.update.GraphStore;
import org.apache.jena.update.GraphStoreFactory;
import org.apache.jena.update.UpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.sparql.WonQueries;

/**
 * Created by fsuda on 04.03.2015.
 */
@Component
public class CLRunnerBean implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CLRunnerBean.class);
    private LinkedDataSource linkedDataSource;

    @Override
    public void run(String... args) throws Exception {
        if (args == null && args.length == 0) {
            logger.warn("arguments: [space-separated list of uris to crawl]");
            return;
        }
        Dataset needDataset = CachingLinkedDataSource.makeDataset();
        for (String arg : args) {
            URI uri = URI.create(arg);
            logger.info("Getting Data from uri: " + uri);
            RdfUtils.addDatasetToDataset(needDataset, linkedDataSource.getDataForResourceWithPropertyPath(uri,
                            configurePropertyPaths(), 10000, 5, false), true);
        }
        logger.info("PRINTING DATASET");
        RDFDataMgr.write(System.out, needDataset, Lang.TRIG);
        logger.info("PRINTED DATASET");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while (true) {
            System.out.print("ENTER SPARQL-QUERY> ");
            line = br.readLine();
            if ("exit".equals(line)) {
                break;
            } else if ("help".equals(line)) {
                printHelp();
            } else if (line.startsWith("%listgraphs")) {
                try {
                    Query query = QueryFactory.create(WonQueries.SPARQL_ALL_GRAPHS);
                    QuerySolutionMap initialBinding = new QuerySolutionMap();
                    // InitialBindings are used to set filters on the resultset
                    // initialBinding.add("need",
                    // needDataset.getDefaultModel().createResource(uri.toString()));
                    try (QueryExecution qExec = QueryExecutionFactory.create(query, needDataset, initialBinding)) {
                        qExec.getContext().set(TDB.symUnionDefaultGraph, true);
                        ResultSet results = qExec.execSelect();
                        printResults(results);
                        qExec.close();
                    }
                } catch (QueryParseException e) {
                    System.out.println("INVALID SPARQL-QUERY: " + e.getMessage());
                    printHelp();
                }
            } else if (line.startsWith("%listall")) {
                try {
                    Query query = QueryFactory.create(WonQueries.SPARQL_ALL_NEEDS);
                    QuerySolutionMap initialBinding = new QuerySolutionMap();
                    // InitialBindings are used to set filters on the resultset
                    // initialBinding.add("need",
                    // needDataset.getDefaultModel().createResource(uri.toString()));
                    try (QueryExecution qExec = QueryExecutionFactory.create(query, needDataset, initialBinding)) {
                        qExec.getContext().set(TDB.symUnionDefaultGraph, true);
                        ResultSet results = qExec.execSelect();
                        printResults(results);
                        qExec.close();
                    }
                } catch (QueryParseException e) {
                    System.out.println("INVALID SPARQL-QUERY: " + e.getMessage());
                    printHelp();
                }
            } else if (line.startsWith("#")) {
                try {
                    String updateString = WonQueries.SPARQL_PREFIX + line.substring(1);
                    GraphStore graphStore = GraphStoreFactory.create(needDataset);
                    UpdateAction.parseExecute(updateString, graphStore);
                } catch (QueryParseException e) {
                    e.printStackTrace();
                    System.out.println("INVALID SPARQL-QUERY: " + e.getMessage());
                    printHelp();
                }
            } else {
                String queryString = WonQueries.SPARQL_PREFIX + line;
                try {
                    Query query = QueryFactory.create(queryString);
                    QuerySolutionMap initialBinding = new QuerySolutionMap();
                    // InitialBindings are used to set filters on the resultset
                    // initialBinding.add("need",
                    // needDataset.getDefaultModel().createResource(uri.toString()));
                    try (QueryExecution qExec = QueryExecutionFactory.create(query, needDataset, initialBinding)) {
                        qExec.getContext().set(TDB.symUnionDefaultGraph, true);
                        ResultSet results = qExec.execSelect();
                        printResults(results);
                        qExec.close();
                    }
                } catch (QueryParseException e) {
                    System.out.println("INVALID SPARQL-QUERY: " + e.getMessage());
                    printHelp();
                }
            }
            // System.out.println(RdfUtils.setSparqlVars(WonQueries.SPARQL_CONNECTIONS_FILTERED_BY_NEED_URI,"need",URI.create("http://rsa021.researchstudio.at:8080/won/resource/need/4871438545203495000")));
        }
    }

    private void printResults(ResultSet results) {
        System.out.println("---------------------------RESULTS-----------------------------------");
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = soln.varNames();
            while (it.hasNext()) {
                String var = it.next();
                sb.append(var).append(": ").append(soln.get(var)).append(" ");
            }
            System.out.println(sb.toString());
        }
        System.out.println("---------------------------------------------------------------------");
    }

    private void printHelp() {
        System.out.println("Enter SPARQL Query (Prefix not needed), or type \"exit\" to exit");
        System.out.println("List all loaded Graphs: \"SELECT DISTINCT ?g WHERE {graph ?g {?s ?p ?o }.}\"");
        System.out.println("Commands > [QUERY]     - executes the given query");
        System.out.println("Commands > #[STMT]     - executes the given statement");
        System.out.println("Commands > %listall    - lists all needs");
        System.out.println("Commands > %listgraphs - lists all graphs");
    }

    /***
     * Build the property paths needed for crawling need data
     */
    private static List<Path> configurePropertyPaths() {
        List<Path> propertyPaths = new ArrayList<Path>();
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">");
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member");
        addPropertyPath(propertyPaths,
                        "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_REMOTE_CONNECTION + ">");
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<"
                        + WON.HAS_EVENT_CONTAINER + ">/rdfs:member");
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<"
                        + WON.HAS_REMOTE_CONNECTION + ">/<" + WON.BELONGS_TO_NEED + ">");
        return propertyPaths;
    }

    private static List<Path> configurePropertyPathAll() {
        List<Path> propertyPaths = new ArrayList<Path>();
        addPropertyPath(propertyPaths, "rdfs:member");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<"
                        + WON.HAS_REMOTE_CONNECTION + ">");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<"
                        + WON.HAS_EVENT_CONTAINER + ">/rdfs:member");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<"
                        + WON.HAS_REMOTE_CONNECTION + ">/<" + WON.BELONGS_TO_NEED + ">");
        return propertyPaths;
    }

    private static void addPropertyPath(final List<Path> propertyPaths, String pathString) {
        Path path = PathParser.parse(pathString, PrefixMapping.Standard);
        propertyPaths.add(path);
    }

    @Autowired
    public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }
}
