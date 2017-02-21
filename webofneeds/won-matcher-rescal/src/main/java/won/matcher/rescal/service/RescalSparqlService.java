package won.matcher.rescal.service;


import com.hp.hpl.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.service.crawler.service.CrawlSparqlService;
import won.matcher.utils.preprocessing.OpenNlpTokenExtraction;
import won.matcher.utils.tensor.TensorMatchingData;

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
    TensorMatchingData matchingData, long fromCrawlDate, long toCrawlDate) {

    // retrieve relevant properties of all needs that match the conditions
    // - crawl status is 'DONE' or 'SAVED'
    // - needs crawl date must be in certain interval
    // - needs must not have the flags 'UsedForTesting' or ''
    String queryString = "prefix won: <http://purl.org/webofneeds/model#>\n" +
      "prefix rdfs: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n\n" +
      "SELECT ?needUri ?type ?wonNodeUri ?title ?desc ?tag WHERE {\n" +
      " ?needUri rdfs:type won:Need.\n" +
      " ?needUri won:crawlStatus ?crawlStatus.\n" +
      " ?needUri won:crawlDate ?date.\n" +
      " ?needUri won:isInState won:Active.\n" +
      " ?needUri won:hasBasicNeedType ?type.\n" +
      " ?needUri won:hasContent/<http://purl.org/dc/elements/1.1/title> ?title.\n" +
      " ?needUri won:hasWonNode ?wonNodeUri.\n" +
      " OPTIONAL {?needUri won:hasContent/won:hasTextDescription ?desc}.\n" +
      " OPTIONAL {?needUri won:hasContent/won:hasTag ?tag}.\n" +
      " FILTER (?date >= " + fromCrawlDate +" && ?date < " + toCrawlDate + " && \n" +
      "        (?crawlStatus = 'DONE' || ?crawlStatus = 'SAVE'))\n}\n";

    log.info("bulk load need data from sparql endpoint in crawlDate range: [{},{}]", fromCrawlDate, toCrawlDate);
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
    String queryString = "prefix won: <http://purl.org/webofneeds/model#>\n" +
      "prefix rdfs: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n\n" +
      "SELECT ?connectionUri ?state ?need1 ?need2 WHERE {\n" +
      " ?connectionUri rdfs:type won:Connection.\n" +
      " ?connectionUri won:crawlStatus 'DONE'.\n" +
      " ?connectionUri won:crawlDate ?date.\n" +
      " ?connectionUri won:hasConnectionState ?state.\n" +
      " ?connectionUri won:belongsToNeed ?need1.\n" +
      " ?connectionUri won:hasRemoteNeed ?need2.\n" +
      " ?rating won:hasBinaryRating won:Good.\n" +
      " ?rating won:forResource ?connectionUri.\n" +
      " FILTER (?date >= " + fromCrawlDate + " && ?date < " + toCrawlDate + ")\n}\n";

    log.info("bulk load connection data from sparql endpoint in crawlDate range: [{},{}]", fromCrawlDate, toCrawlDate);
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
      matchingData.addNeedConnectionIfNeedsExist(need1, need2);
    }
    qexec.close();
    log.info("number of connections loaded: " + numConnections);
  }

}
