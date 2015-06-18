package data;


import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import crawler.msg.CrawlUriMessage;
import crawler.service.CrawlSparqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import preprocessing.OpenNlpTokenExtraction;
import won.protocol.vocabulary.WON;

import java.io.IOException;


/**
 * Service that accesses an SPARQL endpoint where need crawling information is saved.
 * Used to load that data for RESCAL matching.
 *
* User: hfriedrich
* Date: 09.06.2015
*/
public class RescalSparqlService extends CrawlSparqlService
{
  private static final Logger log = LoggerFactory.getLogger(RescalSparqlService.class);
  private OpenNlpTokenExtraction preprocessing;

  public RescalSparqlService(final String sparqlEndpoint) throws IOException {
    super(sparqlEndpoint);
    preprocessing = new OpenNlpTokenExtraction();
  }

  /**
   * Update rescal matching data with need data loaded from the sparql endpoint.
   *
   * @param matchingData matching data to update
   * @param fromCrawlDate minimum crawling timestamp used for loading
   * @param toCrawlDate maximum crawling timestamp used for loading
   */
  public void updateMatchingDataWithActiveNeeds(
    RescalMatchingData matchingData, long fromCrawlDate,long toCrawlDate) {

    // retrieve relevant properties of all needs that match the conditions
    log.info("bulk load need data from sparql endpoint in crawlDate range: [{},{}]", fromCrawlDate, toCrawlDate);
    String queryTemplate = "\nSELECT ?needUri ?type ?title ?desc ?tags WHERE { " +
      " ?needUri <%s> <%s>. ?needUri <%s> '%s'. ?needUri <%s> ?date. " +
      " ?needUri <%s> <%s>. ?needUri <%s> ?type. ?needUri <%s> ?title." +
      " OPTIONAL {?needUri <%s> ?desc}. " + "OPTIONAL {?needUri <%s> ?tags}. " +
      " FILTER (?date >= %d && ?date < %d ) }\n";

    String queryString = String.format(
      queryTemplate, RDF.type, WON.NEED, CrawlSparqlService.CRAWL_STATUS_PREDICATE, CrawlUriMessage.STATUS.DONE,
      CrawlSparqlService.CRAWL_DATE_PREDICATE, WON.IS_IN_STATE, WON.NEED_STATE_ACTIVE,
      WON.HAS_BASIC_NEED_TYPE, WON.HAS_CONTENT.toString() + ">/<" + DC.title.toString(),
      WON.HAS_CONTENT.toString() + ">/<" + WON.HAS_TEXT_DESCRIPTION.toString(),
      WON.HAS_CONTENT.toString() + ">/<" + WON.HAS_TAG.toString(), fromCrawlDate, toCrawlDate);

    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", queryString);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
    ResultSet results = qexec.execSelect();

    log.info("preprocess the loaded need data ...");
    int numNeeds = 0;
    int numAttributes = 0;
    while (results.hasNext()) {

      // add the needs with its attributes to the rescal matching data object
      numNeeds++;
      QuerySolution qs = results.nextSolution();
      String uri = qs.get("needUri").asResource().getURI();
      String type = qs.get("type").asResource().getURI();

      // need type
      matchingData.addNeedType(uri, type);
      numAttributes++;

      // title
      String title = qs.get("title").asLiteral().getString();
      String[] titleTokens = preprocessing.extractWordTokens(title);
      for (String token : titleTokens) {
        matchingData.addNeedAttribute(uri, token, RescalMatchingData.SliceType.TITLE);
        numAttributes++;
      }

      // description
      if (qs.get("desc") != null) {
        String desc = qs.get("desc").asLiteral().getString();
        String[] descTokens = preprocessing.extractRelevantWordTokens(desc);
        for (String token : descTokens) {
          matchingData.addNeedAttribute(uri, token, RescalMatchingData.SliceType.DESCRIPTION);
          numAttributes++;
        }
      }

      // tags
      if ((qs.get("tags") != null) && (qs.get("tags").isLiteral())) {
        String tags = qs.get("tags").asLiteral().getString();
        String[] tagTokens = preprocessing.extractWordTokens(tags);
        for (String token : tagTokens) {
          matchingData.addNeedAttribute(uri, token, RescalMatchingData.SliceType.TAG);
          numAttributes++;
        }
      }
    }
    qexec.close();
    log.info("number of needs loaded: " + numNeeds);
    log.info("number of (possibly double) attributes added: " + numAttributes);
  }

  /**
   * Update rescal matching data with connection data loaded from the sparql endpoint.
   *
   * @param matchingData matching data to update
   * @param fromCrawlDate minimum crawling timestamp used for loading
   * @param toCrawlDate maximum crawling timestamp used for loading
   */
  public void updateMatchingDataWithConnections(
    RescalMatchingData matchingData, long fromCrawlDate,long toCrawlDate) {

    // retrieve relevant properties of all connections that match the conditions
    log.info("bulk load connection data from sparql endpoint in crawlDate range: [{},{}]", fromCrawlDate, toCrawlDate);
    String queryTemplate = "\nSELECT ?connectionUri ?state ?need1 ?need2 WHERE { " +
      "?connectionUri <%s> <%s>. " + "?connectionUri <%s> '%s'. " +
      "?connectionUri <%s> ?date. " + "?connectionUri <%s> ?state. " +
      "?connectionUri <%s> ?need1. " + "?connectionUri <%s> ?need2. " +
      " FILTER (?date >= %d && ?date < %d ) }\n";

    String queryString = String.format(
      queryTemplate, RDF.type, WON.CONNECTION,
      CrawlSparqlService.CRAWL_STATUS_PREDICATE, CrawlUriMessage.STATUS.DONE,
      CrawlSparqlService.CRAWL_DATE_PREDICATE, WON.HAS_CONNECTION_STATE,
      WON.BELONGS_TO_NEED, WON.HAS_REMOTE_NEED,
      fromCrawlDate, toCrawlDate);

    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", queryString);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
    ResultSet results = qexec.execSelect();

    log.info("preprocess the loaded connection data ...");
    int numConnections = 0;
    while (results.hasNext()) {

      // add the needs with its attributes to the rescal matching data object
      numConnections++;
      QuerySolution qs = results.nextSolution();
      String need1 = qs.get("need1").asResource().getURI();
      String need2 = qs.get("need2").asResource().getURI();

      // add the connection to the matching data
      matchingData.addNeedConnection(need1, need2);
    }
    qexec.close();
    log.info("number of connections loaded: " + numConnections);
  }

}
