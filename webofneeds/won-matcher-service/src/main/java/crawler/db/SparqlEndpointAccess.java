package crawler.db;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemote;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import crawler.message.UriStatusMessage;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Access of Sparql enpoint database to save or query linked data.
 *
 * User: hfriedrich
 * Date: 15.04.2015
 */
public class SparqlEndpointAccess
{
  private static final String METADATA_GRAPH = WON.BASE_URI + "crawlMetadata";
  private static final String CRAWL_DATE_PREDICATE = WON.BASE_URI + "crawlDate";
  private static final String CRAWL_STATUS_PREDICATE = WON.BASE_URI + "crawlStatus";
  private static final String CRAWL_BASE_URI_PREDICATE = WON.BASE_URI + "crawlBaseUri";

  private final Logger log = LoggerFactory.getLogger(getClass());
  private String sparqlEndpoint;

  public SparqlEndpointAccess(String sparqlEndpoint) {
    this.sparqlEndpoint = sparqlEndpoint;
  }

  public String getSparqlEndpoint() {
    return sparqlEndpoint;
  }

  private void executeUpdateQuery(String updateQuery) {

    log.debug("Update SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", updateQuery);
    UpdateRequest query = UpdateFactory.create(updateQuery);
    UpdateProcessRemote riStore = (UpdateProcessRemote)
      UpdateExecutionFactory.createRemote(query, sparqlEndpoint);
    riStore.execute();
  }

  /**
   * Update a graph by first clearing it completely and afterwards inserting the triples of the new model.
   *
   * @param graph graph to be updated
   * @param model model that holds triples to set
   */
  public void updateGraph(String graph, Model model) {

    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, model, Lang.NTRIPLES);
    String query = "\nCLEAR GRAPH <" + graph + ">;\n" + "\nINSERT DATA { GRAPH <"+ graph + "> { " + sw + "}};\n";
    executeUpdateQuery(query);
  }

  /**
   * Update the message meta data about the crawling process using a separate graph.
   * For each crawled URI save the date, the current status and the baseUri.
   * This enables to construct the message again (e.g. for executing (unfinished) crawling again later)
   *
   * @param msg message about which crawling meta data is updated
   */
  public void updateCrawlingMetadata(UriStatusMessage msg) {

    // delete the old entry
    String queryTemplate = "\nDELETE WHERE { GRAPH <%s> { <%s> ?y ?z}};";
    String queryString = String.format(queryTemplate, METADATA_GRAPH, msg.getUri());

    // insert now entry
    queryTemplate = "\nINSERT DATA { GRAPH <%s> { <%s> <%s> %d. <%s> <%s> '%s'. <%s> <%s> <%s>}}\n";
    queryString += String.format(queryTemplate, METADATA_GRAPH,
                                 msg.getUri(), CRAWL_DATE_PREDICATE, System.currentTimeMillis(),
                                 msg.getUri(), CRAWL_STATUS_PREDICATE, msg.getStatus(),
                                 msg.getUri(), CRAWL_BASE_URI_PREDICATE, msg.getBaseUri());

    // execute query
    executeUpdateQuery(queryString);
  }

  /**
   * Gets all messages saved in the db of a certain status (e.g. FAILED) and puts them in the STATUS PROCESS to be
   * able to execute the crawling again.
   *
   * @param status
   * @return
   */
  public Set<UriStatusMessage> getMessagesForCrawling(UriStatusMessage.STATUS status) {

    Set<UriStatusMessage> msgs = new HashSet<UriStatusMessage>();
    String queryTemplate = "\nSELECT ?uri ?base WHERE { GRAPH <%s> {?uri ?p '%s'. ?uri <%s> ?base}}\n";
    String queryString = String.format(queryTemplate, METADATA_GRAPH, status, CRAWL_BASE_URI_PREDICATE);
    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", queryString);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
    ResultSet results = qexec.execSelect();

    while (results.hasNext()) {

      QuerySolution qs = results.nextSolution();
      RDFNode uri = qs.get("uri");
      RDFNode baseUri = qs.get("base");
      UriStatusMessage msg = new UriStatusMessage(uri.toString(), baseUri.toString(), UriStatusMessage.STATUS.PROCESS);
      log.debug("Created message: {}", msg);
      msgs.add(msg);
    }
    qexec.close();
    return msgs;
  }

  /**
   * Extract linked URIs of resource URI that are not already crawled. Use specified
   * property paths to construct the query.
   *
   * @param uri current processed resource URI
   * @param properties property paths used to query the sparql endpoint
   * @return set of extracted URIs from the resource URI
   */
  public Set<String> extractURIs(String uri, String baseUri, Iterable<Path> properties) {

    Set<String> extractedURIs = new HashSet<String>();
    for (Path prop : properties) {

      // select URIs specified by property paths that have not already been crawled successfully
      String queryTemplate = "\nSELECT ?obj WHERE { <%s> %s ?obj FILTER NOT EXISTS { <%s> <%s> '%s'}}\n";
      String queryString = String.format(queryTemplate, baseUri, prop.toString(), uri, CRAWL_STATUS_PREDICATE,
                                         UriStatusMessage.STATUS.DONE);
      log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
      log.debug("Execute query: {}", queryString);
      Query query = QueryFactory.create(queryString);
      QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
      ResultSet results = qexec.execSelect();

      while (results.hasNext()) {
        QuerySolution qs = results.nextSolution();
        RDFNode node = qs.get("obj");
        log.debug("Extracted URI: {}", node.toString());
        extractedURIs.add(node.toString());
      }
      qexec.close();
    }

    return extractedURIs;
  }

}
