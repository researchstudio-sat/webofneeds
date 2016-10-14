package won.matcher.service.crawler.service;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.service.sparql.SparqlService;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.protocol.vocabulary.WON;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sparql service extended with methods for crawling
 *
 * User: hfriedrich
 * Date: 04.05.2015
 */
@Component
public class CrawlSparqlService extends SparqlService
{
  private static final String METADATA_GRAPH = WON.BASE_URI + "crawlMetadata";
  private static final String CRAWL_DATE_PREDICATE = WON.BASE_URI + "crawlDate";
  private static final String CRAWL_STATUS_PREDICATE = WON.BASE_URI + "crawlStatus";
  private static final String CRAWL_BASE_URI_PREDICATE = WON.BASE_URI + "crawlBaseUri";
  private static final String CRAWL_WON_NODE_URI_PREDICATE = WON.BASE_URI + "wonNodeUri";

  @Autowired
  public CrawlSparqlService(@Value("${uri.sparql.endpoint}") final String sparqlEndpoint) {
    super(sparqlEndpoint);
  }

  /**
   * Update the message meta data about the crawling process using a separate graph.
   * For each crawled URI save the date, the current status and the baseUri.
   * This enables to construct the message again (e.g. for executing (unfinished)
   * crawling again later)
   *
   * @param msg message that describe crawling meta data to update
   */
  public void updateCrawlingMetadata(CrawlUriMessage msg) {

    // delete the old entry
    String queryTemplate = "\nDELETE WHERE { GRAPH <%s> { <%s> ?y ?z}};";
    String queryString = String.format(queryTemplate, METADATA_GRAPH, msg.getUri());

    // insert new entry
    queryTemplate = "\nINSERT DATA { GRAPH <%s> { <%s> <%s> %d. <%s> <%s> '%s'. <%s> <%s> <%s>. ";
    if (msg.getWonNodeUri() != null) {
      queryTemplate += "<%s> <%s> <%s>. ";
    }
    queryTemplate += "}}\n";
    queryString += String.format(queryTemplate, METADATA_GRAPH,
                                 msg.getUri(), CRAWL_DATE_PREDICATE, msg.getCrawlDate(),
                                 msg.getUri(), CRAWL_STATUS_PREDICATE, msg.getStatus(),
                                 msg.getUri(), CRAWL_BASE_URI_PREDICATE, msg.getBaseUri(),
                                 msg.getUri(), CRAWL_WON_NODE_URI_PREDICATE, msg.getWonNodeUri());

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
  public void bulkUpdateCrawlingMetadata(Collection<CrawlUriMessage> msgs) {

    // delete the old entries
    StringBuilder builder = new StringBuilder();
    for (CrawlUriMessage msg : msgs) {
      builder.append("\nDELETE WHERE { GRAPH  <").append(METADATA_GRAPH).append(">  { <");
      builder.append(msg.getUri()).append("> ?p ?o }};");
    }

    // insert the new entries
    String insertTemplate = "\n<%s> <%s> %d. <%s> <%s> '%s'. <%s> <%s> <%s>. ";
    builder.append("\nINSERT DATA { GRAPH <").append(METADATA_GRAPH).append(">  { ");
    for (CrawlUriMessage msg : msgs) {
      String specificInsertTemplate = insertTemplate;
      if (msg.getWonNodeUri() != null) {
        specificInsertTemplate += "<%s> <%s> <%s>. ";
      }
      builder.append(String.format(specificInsertTemplate, msg.getUri(), CRAWL_DATE_PREDICATE, msg.getCrawlDate(),
                                   msg.getUri(), CRAWL_STATUS_PREDICATE, msg.getStatus(),
                                   msg.getUri(), CRAWL_BASE_URI_PREDICATE, msg.getBaseUri(),
                                   msg.getUri(), CRAWL_WON_NODE_URI_PREDICATE, msg.getWonNodeUri()));
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
  public Set<CrawlUriMessage> retrieveMessagesForCrawling(CrawlUriMessage.STATUS status) {

    Set<CrawlUriMessage> msgs = new LinkedHashSet<>();
    String queryTemplate = "\nSELECT ?uri ?base ?wonNode WHERE { GRAPH <%s> " +
      "{?uri ?p '%s'. ?uri <%s> ?base OPTIONAL { ?uri <%s> ?wonNode } }}\n";
    String queryString = String.format(queryTemplate, METADATA_GRAPH, status, CRAWL_BASE_URI_PREDICATE, CRAWL_WON_NODE_URI_PREDICATE);
    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", queryString);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
    ResultSet results = qexec.execSelect();

    while (results.hasNext()) {

      QuerySolution qs = results.nextSolution();
      String uri = qs.get("uri").asResource().getURI();
      String baseUri = qs.get("base").asResource().getURI();
      CrawlUriMessage msg = null;
      if (qs.get("wonNode") != null) {
        String wonNode = qs.get("wonNode").asResource().getURI();
        msg = new CrawlUriMessage(uri, baseUri, wonNode, CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis());
      } else {
        msg = new CrawlUriMessage(uri, baseUri, CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis());
      }

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
  public Set<String> extractURIs(String uri, String baseUri, Iterable<String> properties) {
    Set<String> extractedURIs = new HashSet<String>();
    for (String prop : properties) {
      if (prop.trim().length()==0) {
        continue;
        //ignore empty strings
      }
      // select URIs specified by property paths that have not already been crawled
      String queryTemplate = "\nSELECT ?obj WHERE { <%s> %s ?obj " +
        "FILTER NOT EXISTS { { <%s> <%s> '%s' } UNION { <%s> <%s> '%s'} " +
        "UNION { ?obj <%s> ?any } } }\n";
      String queryString = String.format(queryTemplate, baseUri, prop,
                                         uri, CRAWL_STATUS_PREDICATE, CrawlUriMessage.STATUS.DONE,
                                         uri, CRAWL_STATUS_PREDICATE, CrawlUriMessage.STATUS.FAILED,
                                         CRAWL_STATUS_PREDICATE);

      log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
      log.debug("Execute query: {}", queryString);
      Query query = QueryFactory.create(queryString);
      QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
      ResultSet results = qexec.execSelect();

      while (results.hasNext()) {
        QuerySolution qs = results.nextSolution();
        String extractedUri = qs.get("obj").asResource().getURI();
        log.debug("Extracted URI: {}", extractedUri);
        extractedURIs.add(extractedUri);
      }
      qexec.close();
    }

    return extractedURIs;
  }

  public BulkNeedEvent retrieveNeedEvents(long fromDate, long toDate, int maxEvents) {

    // retrieve relevant properties of all needs that match the conditions
    log.debug("bulk load need data from sparql endpoint in date range: [{},{}]", fromDate, toDate);
    String queryTemplate = "\nSELECT ?needUri ?type ?wonNodeUri ?title ?desc ?tags WHERE { " +
      " ?needUri <%s> <%s>. ?needUri <%s> '%s'. ?needUri <%s> ?date. " +
      " ?needUri <%s> <%s>. ?needUri <%s> ?type. ?needUri <%s> ?title." +
      " ?needUri <%s> ?wonNodeUri." +
      " OPTIONAL {?needUri <%s> ?desc}. " + "OPTIONAL {?needUri <%s> ?tags}. " +
      " FILTER (?date >= %d && ?date < %d ) }\n";

    String queryString = String.format(
      queryTemplate, RDF.type, WON.NEED, CrawlSparqlService.CRAWL_STATUS_PREDICATE, CrawlUriMessage.STATUS.DONE,
      CrawlSparqlService.CRAWL_DATE_PREDICATE, WON.IS_IN_STATE, WON.NEED_STATE_ACTIVE,
      WON.HAS_BASIC_NEED_TYPE, WON.HAS_CONTENT.toString() + ">/<" + DC.title.toString(),
      WON.HAS_WON_NODE,
      WON.HAS_CONTENT.toString() + ">/<" + WON.HAS_TEXT_DESCRIPTION.toString(),
      WON.HAS_CONTENT.toString() + ">/<" + WON.HAS_TAG.toString(), fromDate, toDate);

    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", queryString);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
    ResultSet results = qexec.execSelect();

    int numNeeds = 0;
    while (results.hasNext()) {

      // add the needs with its attributes to the rescal matching data object
      numNeeds++;
      QuerySolution qs = results.nextSolution();
      String needUri = qs.get("needUri").asResource().getURI();
      String wonNodeUri = qs.get("wonNodeUri").asResource().getURI();
      String type = qs.get("type").asLiteral().getString();

      log.info("loaded: {}, {}, {}", needUri, type);

    }
    qexec.close();
    log.debug("number of needs loaded: " + numNeeds);
    return null;
  }

}
