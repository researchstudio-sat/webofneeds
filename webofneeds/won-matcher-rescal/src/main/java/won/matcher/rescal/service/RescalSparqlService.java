package won.matcher.rescal.service;


import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.matcher.service.crawler.service.CrawlSparqlService;
import won.matcher.utils.preprocessing.OpenNlpTokenExtraction;
import won.matcher.utils.tensor.TensorMatchingData;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


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
    TensorMatchingData matchingData, long fromCrawlDate,long toCrawlDate) {

    // retrieve relevant properties of all needs that match the conditions
    // - crawl status is 'DONE' or 'SAVED'
    // - needs crawl date must be in certain interval
    log.info("bulk load need data from sparql endpoint in crawlDate range: [{},{}]", fromCrawlDate, toCrawlDate);
    String queryTemplate = "\nSELECT ?needUri ?type ?wonNodeUri ?title ?desc ?tag WHERE {\n" +
      " ?needUri <%s> <%s>.\n ?needUri <%s> ?crawlStatus.\n ?needUri <%s> ?date.\n" +
      " ?needUri <%s> <%s>.\n ?needUri <%s> ?type.\n ?needUri <%s> ?title.\n" +
      " ?needUri <%s> ?wonNodeUri.\n" +
      " OPTIONAL {?needUri <%s> ?desc}.\n" + " OPTIONAL {?needUri <%s> ?tag}.\n" +
      " FILTER (?date >= %d && ?date < %d && (?crawlStatus = '%s' || ?crawlStatus = '%s')) }\n";

    String queryString = String.format(
      queryTemplate, RDF.type, WON.NEED, CrawlSparqlService.CRAWL_STATUS_PREDICATE,
      CrawlSparqlService.CRAWL_DATE_PREDICATE, WON.IS_IN_STATE, WON.NEED_STATE_ACTIVE,
      WON.HAS_BASIC_NEED_TYPE, WON.HAS_CONTENT.toString() + ">/<" + DC.title.toString(),
      WON.HAS_WON_NODE,
      WON.HAS_CONTENT.toString() + ">/<" + WON.HAS_TEXT_DESCRIPTION.toString(),
      WON.HAS_CONTENT.toString() + ">/<" + WON.HAS_TAG.toString(), fromCrawlDate, toCrawlDate,
      CrawlUriMessage.STATUS.DONE, CrawlUriMessage.STATUS.SAVE);

    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", queryString);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
    ResultSet results = qexec.execSelect();

    log.info("preprocess the loaded need data ...");
    int numNeeds = 0;
    int numAttributes = 0;
    Set<String> processedNeedUris = new HashSet<>();


    while (results.hasNext()) {

      // add the needs with its attributes to the rescal matching data object
      numNeeds++;
      QuerySolution qs = results.nextSolution();
      String needUri = qs.get("needUri").asResource().getURI();
      String type = qs.get("type").asResource().getURI();

      // needUris can occur multiple times in the query solution set since there is an entry for every tag of the
      // need. if we already processed this needUri in this solution set we can skip everything except the tag
      if (!processedNeedUris.contains(needUri)) {
        log.debug("processing need: {}", needUri);

        // need type
        matchingData.addNeedType(needUri, type);
        numAttributes++;

        // title
        String title = qs.get("title").asLiteral().getString();
        String[] titleTokens = preprocessing.extractWordTokens(title);
        for (String token : titleTokens) {
          matchingData.addNeedAttribute(needUri, token, TensorMatchingData.SliceType.TITLE);
          numAttributes++;
        }

        // won node of need
        String wonNodeUri = qs.get("wonNodeUri").asResource().getURI();
        matchingData.setWonNodeOfNeed(needUri, wonNodeUri);

        // description
        if (qs.get("desc") != null) {
          String desc = qs.get("desc").asLiteral().getString();
          String[] descTokens = preprocessing.extractWordTokens(desc);
          for (String token : descTokens) {
            matchingData.addNeedAttribute(needUri, token, TensorMatchingData.SliceType.DESCRIPTION);
            numAttributes++;
          }
        }
      }

      // tag
      if ((qs.get("tag") != null) && (qs.get("tag").isLiteral())) {
        String tag = qs.get("tag").asLiteral().getString();
        matchingData.addNeedAttribute(needUri, tag, TensorMatchingData.SliceType.TAG);
        numAttributes++;
      }

      // add the need uri to the processed ones
      processedNeedUris.add(needUri);
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
    TensorMatchingData matchingData, long fromCrawlDate,long toCrawlDate) {

    // retrieve relevant properties of all connections that match the conditions:
    // - use all connections for learning with Good Feedback (this automatically excludes hints)
    // - connections crawl date must be in certain interval
    log.info("bulk load connection data from sparql endpoint in crawlDate range: [{},{}]", fromCrawlDate, toCrawlDate);
    String queryTemplate = "\nSELECT ?connectionUri ?state ?need1 ?need2 WHERE {\n" +
      " ?connectionUri <%s> <%s>.\n ?connectionUri <%s> '%s'.\n" +
      " ?connectionUri <%s> ?date.\n ?connectionUri <%s> ?state.\n" +
      " ?connectionUri <%s> ?need1.\n ?connectionUri <%s> ?need2.\n" +
      " ?rating <%s> <%s>.\n ?rating <%s> ?connectionUri.\n" +
      " FILTER (?date >= %d && ?date < %d) }\n";

    String queryString = String.format(
      queryTemplate, RDF.type, WON.CONNECTION,
      CrawlSparqlService.CRAWL_STATUS_PREDICATE, CrawlUriMessage.STATUS.DONE,
      CrawlSparqlService.CRAWL_DATE_PREDICATE, WON.HAS_CONNECTION_STATE,
      WON.BELONGS_TO_NEED, WON.HAS_REMOTE_NEED,
      WON.HAS_BINARY_RATING, WON.GOOD, WON.FOR_RESOURCE,
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
