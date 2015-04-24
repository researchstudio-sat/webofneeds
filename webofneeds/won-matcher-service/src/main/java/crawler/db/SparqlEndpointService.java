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
import java.util.*;

/**
 * Service to access of Sparql enpoint database to save or query linked data.
 *
 * User: hfriedrich
 * Date: 15.04.2015
 */
public class SparqlEndpointService
{
  private static final String METADATA_GRAPH = WON.BASE_URI + "crawlMetadata";
  private static final String CRAWL_DATE_PREDICATE = WON.BASE_URI + "crawlDate";
  private static final String CRAWL_STATUS_PREDICATE = WON.BASE_URI + "crawlStatus";
  private static final String CRAWL_BASE_URI_PREDICATE = WON.BASE_URI + "crawlBaseUri";

  private final Logger log = LoggerFactory.getLogger(getClass());
  private String sparqlEndpoint;

  public SparqlEndpointService(String sparqlEndpoint) {
    this.sparqlEndpoint = sparqlEndpoint;
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
   * Update a graph by first deleting it and afterwards inserting the triples of the new model.
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
   * Update a dataset of graphs first deleting them and afterwards inserting the triples of the new models.
   *
   * @param ds
   */
  public void updateDataset(Dataset ds) {

    Iterator<String> graphNames = ds.listNames();
    while (graphNames.hasNext()) {

      log.debug("Save dataset");
      String graphName = graphNames.next();
      Model model = ds.getNamedModel(graphName);
      updateGraph(graphName, model);
    }
  }

  /**
   * Update the message meta data about the crawling process using a separate graph.
   * For each crawled URI save the date, the current status and the baseUri.
   * This enables to construct the message again (e.g. for executing (unfinished)
   * crawling again later)
   *
   * @param msg message that describe crawling meta data to update
   */
  public void updateCrawlingMetadata(UriStatusMessage msg) {

    // delete the old entry
    String queryTemplate = "\nDELETE WHERE { GRAPH <%s> { <%s> ?y ?z}};";
    String queryString = String.format(queryTemplate, METADATA_GRAPH, msg.getUri());

    // insert new entry
    queryTemplate = "\nINSERT DATA { GRAPH <%s> { <%s> <%s> %d. <%s> <%s> '%s'. <%s> <%s> <%s> }}\n";
    queryString += String.format(queryTemplate, METADATA_GRAPH,
                                 msg.getUri(), CRAWL_DATE_PREDICATE, System.currentTimeMillis(),
                                 msg.getUri(), CRAWL_STATUS_PREDICATE, msg.getStatus(),
                                 msg.getUri(), CRAWL_BASE_URI_PREDICATE, msg.getBaseUri());

    // execute query
    executeUpdateQuery(queryString);
  }

  /**
   * Bulk update of several meta data messages about the crawling process using a separate graph.
   * For each crawled URI save the date, the current status and the baseUri using only a
   * single Sparql update query.
   * This enables to construct the message again (e.g. for executing (unfinished)
   * crawling again later).
   *
   * @param msgs multiple messages that describe crawling meta data to update
   */
  public void bulkUpdateCrawlingMetadata(Collection<UriStatusMessage> msgs) {

    // delete the old entries
    StringBuilder builder = new StringBuilder();
    for (UriStatusMessage msg : msgs) {
      builder.append("\nDELETE WHERE { GRAPH  <").append(METADATA_GRAPH).append(">  { <");
      builder.append(msg.getUri()).append("> ?p ?o }};");
    }

    // insert the new entries
    String insertTemplate = "\n<%s> <%s> %d. <%s> <%s> '%s'. <%s> <%s> <%s>. ";
    builder.append("\nINSERT DATA { GRAPH <").append(METADATA_GRAPH).append(">  { ");
    for (UriStatusMessage msg : msgs) {
      builder.append(String.format(insertTemplate, msg.getUri(), CRAWL_DATE_PREDICATE, System.currentTimeMillis(),
                                   msg.getUri(), CRAWL_STATUS_PREDICATE, msg.getStatus(),
                                   msg.getUri(), CRAWL_BASE_URI_PREDICATE, msg.getBaseUri()));
    }
    builder.append("}};\n");

    // execute the bulk query
    executeUpdateQuery(builder.toString());
  }

  /**
   * Gets all messages saved in the db of a certain status (e.g. FAILED) and puts
   * them in the STATUS PROCESS to be able to execute the crawling again.
   *
   * @param status
   * @return
   */
  public Set<UriStatusMessage> getMessagesForCrawling(UriStatusMessage.STATUS status) {

    Set<UriStatusMessage> msgs = new LinkedHashSet<>();
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

      // select URIs specified by property paths that have not already been crawled
      String queryTemplate = "\nSELECT ?obj WHERE { <%s> %s ?obj " +
        "FILTER NOT EXISTS { { <%s> <%s> '%s' } UNION { <%s> <%s> '%s'} " +
        "UNION { ?obj <%s> ?any } } }\n";
      String queryString = String.format(queryTemplate, baseUri, prop.toString(),
                                         uri, CRAWL_STATUS_PREDICATE, UriStatusMessage.STATUS.DONE,
                                         uri, CRAWL_STATUS_PREDICATE, UriStatusMessage.STATUS.FAILED,
                                         CRAWL_STATUS_PREDICATE);

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
