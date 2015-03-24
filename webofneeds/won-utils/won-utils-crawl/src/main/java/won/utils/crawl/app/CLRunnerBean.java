package won.utils.crawl.app;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import com.hp.hpl.jena.tdb.TDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import won.protocol.model.BasicNeedType;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.sparql.WonQueries;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by fsuda on 04.03.2015.
 */
@Component
public class CLRunnerBean implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CLRunnerBean.class);
    private LinkedDataSource linkedDataSource;

    @Override
    public void run(String... args) throws Exception {
        if(args == null && args.length == 0){
            logger.info("No Arguments present");
            return;
        }

        Dataset needDataset = CachingLinkedDataSource.makeDataset();

        for(String arg : args) {
            URI uri = URI.create(arg);
            logger.info("Getting Data from uri: " + uri);
            RdfUtils.addDatasetToDataset(needDataset, linkedDataSource.getDataForResourceWithPropertyPath(uri, configurePropertyPaths(), 10000, 4, false),true);
        }
        //RDFDataMgr.write(System.err, needDataset, Lang.TRIG); //THIS IS TO PRINT THE WHOLE RDF

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while(true){
            System.out.print("ENTER SPARQL-QUERY> ");
            line = br.readLine();

            if("exit".equals(line)){
                break;
            }else if("help".equals(line)){
                printHelp();
            }else {
                String queryString = WonQueries.SPARQL_PREFIX + line;
                try {
                    Query query = QueryFactory.create(queryString);
                    QuerySolutionMap initialBinding = new QuerySolutionMap();
                    // InitialBindings are used to set filters on the resultset
                    //initialBinding.add("need", needDataset.getDefaultModel().createResource(uri.toString()));

                    QueryExecution qExec = QueryExecutionFactory.create(query, needDataset, initialBinding);

                    qExec.getContext().set(TDB.symUnionDefaultGraph, true);
                    ResultSet results = qExec.execSelect();


                    printResults(results);
                    qExec.close();
                } catch (QueryParseException e) {
                    System.out.println("INVALID SPARQL-QUERY: " + e.getMessage());
                    printHelp();
                }
            }

            System.out.println(RdfUtils.setSparqlVars(WonQueries.SPARQL_CONNECTIONS_FILTERED_BY_NEED_URI,"need",URI.create("http://rsa021.researchstudio.at:8080/won/resource/need/4871438545203495000")));
        }
    }

    private void printResults(ResultSet results){
        System.out.println("---------------------------RESULTS-----------------------------------");
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();

            StringBuilder sb = new StringBuilder();
            Iterator<String> it = soln.varNames();

            while(it.hasNext()){
                String var = it.next();

                if("type".equals(var)){
                    BasicNeedType.fromURI(URI.create(soln.get(var).asResource().getURI()));
                }
                sb.append(var).append(": ").append(soln.get(var)).append(" ");
            }
            System.out.println(sb.toString());
        }
        System.out.println("---------------------------------------------------------------------");
    }

    private void printHelp(){
        System.out.println("Enter SPARQL Query (Prefix not needed), or type \"exit\" to exit");
        System.out.println("List all loaded Graphs: \"SELECT DISTINCT ?g WHERE {graph ?g {?s ?p ?o }.}\"");
    }

    /***
     * Build the property paths needed for crawling need data
     */
    private static List<Path> configurePropertyPaths(){
        List<Path> propertyPaths = new ArrayList<Path>();
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">");
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member");
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_REMOTE_CONNECTION + ">");
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_EVENT_CONTAINER + ">/rdfs:member");
        addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_REMOTE_CONNECTION + ">/<" +WON.BELONGS_TO_NEED + ">");
        return propertyPaths;
    }

    private static List<Path> configurePropertyPathAll(){
        List<Path> propertyPaths = new ArrayList<Path>();
        addPropertyPath(propertyPaths, "rdfs:member");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_REMOTE_CONNECTION + ">");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_EVENT_CONTAINER + ">/rdfs:member");
        addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_REMOTE_CONNECTION + ">/<" +WON.BELONGS_TO_NEED + ">");
        return propertyPaths;
    }

    private static void addPropertyPath(final List<Path> propertyPaths, String pathString) {
        Path path = PathParser.parse(pathString, PrefixMapping.Standard);
        propertyPaths.add(path);
    }

    @Autowired
    public void setLinkedDataSource(final LinkedDataSource linkedDataSource) { this.linkedDataSource = linkedDataSource; }
}

