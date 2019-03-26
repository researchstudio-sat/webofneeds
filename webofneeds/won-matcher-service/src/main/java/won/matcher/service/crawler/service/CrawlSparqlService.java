package won.matcher.service.crawler.service;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.service.sparql.SparqlService;
import won.matcher.service.crawler.config.CrawlConfig;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.protocol.util.NeedModelWrapper;

/**
 * Sparql service extended with methods for crawling
 * <p>
 * User: hfriedrich Date: 04.05.2015
 */
@Component
public class CrawlSparqlService extends SparqlService {

  private static final String HTTP_HEADER_SEPARATOR = ", ";

  @Autowired
  public CrawlSparqlService(@Value("${uri.sparql.endpoint}") final String sparqlEndpoint) {
    super(sparqlEndpoint);
  }

  @Autowired
  private CrawlConfig config;

  /**
   * Update the message meta data about the crawling process using a separate
   * graph.
   * 
   * @param msg message that describe crawling meta data to update
   */
  public void updateCrawlingMetadata(CrawlUriMessage msg) {
    executeUpdateQuery(createUpdateCrawlingMetadataQuery(msg));
  }

  /**
   * Bulk update of several meta data messages about the crawling process using a
   * separate graph.
   * 
   * @param msgs multiple messages that describe crawling meta data to update
   */
  public void bulkUpdateCrawlingMetadata(Collection<CrawlUriMessage> msgs) {

    StringBuilder builder = new StringBuilder();
    for (CrawlUriMessage msg : msgs) {
      builder.append(createUpdateCrawlingMetadataQuery(msg));
    }

    // execute the bulk query
    executeUpdateQuery(builder.toString());
  }

  private String createUpdateCrawlingMetadataQuery(CrawlUriMessage msg) {

    // delete the old entry
    StringBuilder builder = new StringBuilder();
    builder.append("DELETE WHERE { GRAPH won:crawlMetadata { ?msgUri ?y ?z}};\n");

    // insert new entry
    builder.append("\nINSERT DATA { GRAPH won:crawlMetadata {\n");
    builder.append("?msgUri won:crawlDate ?crawlDate.\n");
    builder.append("?msgUri won:crawlStatus ?crawlStatus.\n");
    builder.append("?msgUri won:crawlBaseUri ?crawlBaseUri.\n");

    if (msg.getWonNodeUri() != null) {
      builder.append("?msgUri won:wonNodeUri ?wonNodeUri.\n");
    }

    if (msg.getResourceETagHeaderValues() != null && !msg.getResourceETagHeaderValues().isEmpty()) {
      for (int i = 0; i < msg.getResourceETagHeaderValues().size(); i++) {
        builder.append("?msgUri won:resourceETagValue ? .\n");
      }
    }

    builder.append("}};\n");

    ParameterizedSparqlString pss = new ParameterizedSparqlString();
    pss.setCommandText(builder.toString());
    pss.setNsPrefix("won", "http://purl.org/webofneeds/model#");
    pss.setIri("msgUri", msg.getUri());
    pss.setLiteral("crawlDate", msg.getCrawlDate());
    pss.setLiteral("crawlStatus", msg.getStatus().toString());
    pss.setIri("crawlBaseUri", msg.getBaseUri());
    if (msg.getWonNodeUri() != null) {
      pss.setIri("wonNodeUri", msg.getWonNodeUri());
    }

    if (msg.getResourceETagHeaderValues() != null && !msg.getResourceETagHeaderValues().isEmpty()) {
      int i = 0;
      for (String etagValue : msg.getResourceETagHeaderValues()) {
        pss.setLiteral(i, etagValue);
        i++;
      }
    }

    return pss.toString();
  }

  private Set<String> commaConcatenatedStringToSet(String contatenatedString) {

    if (contatenatedString == null || contatenatedString.isEmpty()) {
      return null;
    }

    String[] splitValues = StringUtils.split(contatenatedString, HTTP_HEADER_SEPARATOR);
    if (splitValues == null) {
      return new HashSet<>(Arrays.asList(contatenatedString));
    }

    return new HashSet(Arrays.asList(splitValues));
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

    String queryString = "SELECT ?uri ?base ?wonNode (group_concat(distinct ?etag;separator=\"" + HTTP_HEADER_SEPARATOR
        + "\") as ?etags)" + " WHERE { GRAPH won:crawlMetadata {\n" + " ?uri ?p ?status.\n"
        + " ?uri won:crawlBaseUri ?base.\n" + " OPTIONAL { ?uri won:wonNodeUri ?wonNode }\n"
        + " OPTIONAL { ?uri won:resourceETagValue ?etag }}}\n" + " GROUP BY ?uri ?base ?wonNode\n";

    ParameterizedSparqlString pps = new ParameterizedSparqlString();
    pps.setNsPrefix("won", "http://purl.org/webofneeds/model#");
    pps.setCommandText(queryString);
    pps.setLiteral("status", status.toString());

    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", pps.toString());
    try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, pps.asQuery())) {
      ResultSet results = qexec.execSelect();

      while (results.hasNext()) {

        QuerySolution qs = results.nextSolution();
        String uri = qs.get("uri").asResource().getURI();
        String baseUri = qs.get("base").asResource().getURI();
        CrawlUriMessage msg = null;
        String wonNode = null;
        Set<String> etags = null;

        if (qs.get("wonNode") != null) {
          wonNode = qs.get("wonNode").asResource().getURI();
        }

        if (qs.get("etags") != null) {
          String etagsString = qs.get("etags").asLiteral().getString();
          etags = commaConcatenatedStringToSet(etagsString);
        }

        msg = new CrawlUriMessage(uri, baseUri, wonNode, CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis(),
            etags);
        log.debug("Created message: {}", msg);
        msgs.add(msg);
      }
      return msgs;
    }
  }

  /**
   * Extract linked URIs of resource URI and create new CrawlUriMessages out of it
   * for crawling. Uses base and non-base property paths for the extraction of
   * uris and creation of new crawling messages.
   *
   * @param baseUri    base uri of the current processed resource uri message
   * @param wonNodeUri won node rui of the current processed resource uri message
   * @return set of extracted CrawlUriMessages
   */
  public Set<CrawlUriMessage> extractCrawlUriMessages(String baseUri, String wonNodeUri) {

    Set<CrawlUriMessage> newCrawlMessages = new HashSet<CrawlUriMessage>();

    // extract uris from non-base property path
    for (String prop : config.getCrawlNonBasePropertyPaths()) {
      Set<CrawlUriMessage> msgs = extractCrawlUriMessagesForPropertyPath(baseUri, wonNodeUri, prop, false);
      if (msgs != null) {
        newCrawlMessages.addAll(msgs);
      }
    }

    // extract uris from base property paths
    for (String prop : config.getCrawlBasePropertyPaths()) {
      Set<CrawlUriMessage> msgs = extractCrawlUriMessagesForPropertyPath(baseUri, wonNodeUri, prop, true);
      if (msgs != null) {
        newCrawlMessages.addAll(msgs);
      }
    }

    return newCrawlMessages;
  }

  /**
   * Extract linked URIs of resource URI and create new CrawlUriMessages for a
   * certain property path and a base Uri. Also extract ETag values if they are
   * available for certain uri resources so that they can be used to make crawling
   * more efficient. Use specified property paths to construct the query.
   *
   * @param baseUri      base uri of the current processed resource uri message
   * @param wonNodeUri   won node rui of the current processed resource uri
   *                     message
   * @param propertyPath property path used to extract new uris in conjunction
   *                     with base uri
   * @param baseProperty base uri used to extract new uris in conjunction property
   *                     path
   * @return set of CrawlUriMessages extracted using a certain base uri and
   *         property path
   */
  private Set<CrawlUriMessage> extractCrawlUriMessagesForPropertyPath(String baseUri, String wonNodeUri,
      String propertyPath, boolean baseProperty) {

    if (propertyPath.trim().length() == 0) {
      return null;
    }

    // select URIs specified by property paths that have not already been crawled
    Set<CrawlUriMessage> newCrawlMessages = new HashSet<CrawlUriMessage>();
    long crawlDate = System.currentTimeMillis();

    // we have to query the baseUri with and without trailing slahes cause we don't
    // know how the RDF data
    // is described in detail. Usually the "need" prefix ends with a trailing
    // "slash" but we don't assume
    // here that is always the case, so we query both variants: with and without
    // trailing slashes.
    // Check the need list with its need: rdfs:member entries for example
    String queryString = "SELECT ?uri (group_concat(distinct ?etag;separator=\"" + HTTP_HEADER_SEPARATOR
        + "\") as ?etags) WHERE {\n" + "{ ?baseUriWithTrailingSlash " + propertyPath + " ?uri. } \n" + // propertyPath
                                                                                                       // has to be
                                                                                                       // appended
                                                                                                       // manually
                                                                                                       // because it
                                                                                                       // contains ">"
                                                                                                       // character and
                                                                                                       // ParameterizedSparqlString
                                                                                                       // cause of
                                                                                                       // injection risk
        "UNION { ?baseUriWithoutTrailingSlash " + propertyPath + " ?uri. } \n" + // propertyPath has to be appended
                                                                                 // manually because it contains ">"
                                                                                 // character and
                                                                                 // ParameterizedSparqlString cause of
                                                                                 // injection risk
        " OPTIONAL {?uri won:resourceETagValue ?etag. }}\n" + " GROUP BY ?uri\n";

    ParameterizedSparqlString pps = new ParameterizedSparqlString();
    pps.setNsPrefix("won", "http://purl.org/webofneeds/model#");
    pps.setCommandText(queryString);

    baseUri = baseUri.trim();
    if (baseUri.endsWith("/")) {
      baseUri = baseUri.substring(0, baseUri.length() - 1);
    }

    pps.setIri("baseUriWithoutTrailingSlash", baseUri);
    pps.setIri("baseUriWithTrailingSlash", baseUri + "/");

    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", pps.toString());
    try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, pps.asQuery())) {
      ResultSet results = qexec.execSelect();

      while (results.hasNext()) {
        QuerySolution qs = results.nextSolution();
        String extractedUri = qs.get("uri").asResource().getURI();

        Set<String> etags = null;
        if (qs.get("etags") != null) {
          String etagsString = qs.get("etags").asLiteral().getString();
          etags = commaConcatenatedStringToSet(etagsString);
        }

        CrawlUriMessage newUriMsg = null;
        log.debug("Extracted URI: {}", extractedUri);
        if (baseProperty) {
          newUriMsg = new CrawlUriMessage(extractedUri, extractedUri, wonNodeUri, CrawlUriMessage.STATUS.PROCESS,
              crawlDate, etags);
        } else {
          newUriMsg = new CrawlUriMessage(extractedUri, baseUri, wonNodeUri, CrawlUriMessage.STATUS.PROCESS, crawlDate,
              etags);
        }
        newCrawlMessages.add(newUriMsg);
      }
      return newCrawlMessages;
    }
  }

  /**
   * To start crawling (http modification query) from a certain point in time,
   * take last modification date from a need known in the database that is in
   * status 'DONE' which means it has been crawled.
   *
   * @param wonNodeUri won node uri for which need modification dates should be
   *                   retrieved
   * @return modification date to start crawling from or null if none exists
   */
  public String retrieveNeedModificationDateForCrawling(String wonNodeUri) {

    String queryString = "SELECT ?modificationDate WHERE {\n" + " ?needUri a won:Need.\n"
        + " ?needUri won:hasWonNode ?wonNodeUri. \n" + " ?needUri dcterms:modified ?modificationDate. \n"
        + " ?needUri won:crawlStatus 'DONE'. \n" + "} ORDER BY DESC(?modificationDate) LIMIT 1\n";

    ParameterizedSparqlString pps = new ParameterizedSparqlString();
    pps.setNsPrefix("won", "http://purl.org/webofneeds/model#");
    pps.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
    pps.setCommandText(queryString);
    pps.setIri("wonNodeUri", wonNodeUri);

    try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, pps.asQuery())) {
      ResultSet results = qexec.execSelect();
      String modificationDate = null;
      if (results.hasNext()) {
        QuerySolution qs = results.nextSolution();
        modificationDate = qs.get("modificationDate").asLiteral().getString();
      }
      return modificationDate;
    }
  }

  /**
   * To start crawling (http modification query) from a certain point in time,
   * take last modification date from a connection known in the database that is
   * in status 'DONE' which means it has been crawled.
   *
   * @param wonNodeUri won node uri for which connection modification dates should
   *                   be retrieved
   * @return modification date to start crawling from or null if none exists
   */
  public String retrieveConnectionModificationDateForCrawling(String wonNodeUri) {

    String queryString = "SELECT ?modificationDate WHERE {\n" + " ?connectionUri a won:Connection.\n"
        + " ?connectionUri won:hasWonNode ?wonNodeUri. \n" + " ?connectionUri dcterms:modified ?modificationDate. \n"
        + " ?connectionUri won:crawlStatus 'DONE'. \n" + "} ORDER BY DESC(?modificationDate) LIMIT 1\n";

    ParameterizedSparqlString pps = new ParameterizedSparqlString();
    pps.setNsPrefix("won", "http://purl.org/webofneeds/model#");
    pps.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
    pps.setCommandText(queryString);
    pps.setIri("wonNodeUri", wonNodeUri);

    try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, pps.asQuery())) {
      ResultSet results = qexec.execSelect();
      String modificationDate = null;
      if (results.hasNext()) {
        QuerySolution qs = results.nextSolution();
        modificationDate = qs.get("modificationDate").asLiteral().getString();
      }
      return modificationDate;
    }
  }

  public BulkNeedEvent retrieveActiveNeedEvents(long fromDate, long toDate, int offset, int limit,
      boolean sortAscending) {

    // query template to retrieve all alctive cralwed/saved needs in a certain date
    // range
    String orderClause = sortAscending ? "ORDER BY ?date\n" : "ORDER BY DESC(?date)\n";
    log.debug("bulk load need data from sparql endpoint in date range: [{},{}]", fromDate, toDate);

    String queryTemplate = "SELECT ?needUri ?wonNodeUri ?date WHERE {  \n" + "  ?needUri a won:Need. \n"
        + "  ?needUri won:crawlDate ?date.  \n" + "  ?needUri won:isInState won:Active. \n"
        + "  ?needUri won:hasWonNode ?wonNodeUri. \n"
        + "  {?needUri won:crawlStatus 'SAVE'.} UNION {?needUri won:crawlStatus 'DONE'.}\n"
        + "  FILTER (?date >= ?fromDate && ?date < ?toDate ) \n" + "} " + orderClause + " OFFSET ?offset\n"
        + " LIMIT ?limit";

    ParameterizedSparqlString pps = new ParameterizedSparqlString();
    pps.setNsPrefix("won", "http://purl.org/webofneeds/model#");
    pps.setCommandText(queryTemplate);
    pps.setLiteral("fromDate", fromDate);
    pps.setLiteral("toDate", toDate);
    pps.setLiteral("offset", offset);
    pps.setLiteral("limit", limit);

    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", pps.toString());
    try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, pps.asQuery())) {
      ResultSet results = qexec.execSelect();

      // load all the needs into one bulk need event
      BulkNeedEvent bulkNeedEvent = new BulkNeedEvent();
      while (results.hasNext()) {

        QuerySolution qs = results.nextSolution();
        String needUri = qs.get("needUri").asResource().getURI();
        String wonNodeUri = qs.get("wonNodeUri").asResource().getURI();
        long crawlDate = qs.getLiteral("date").getLong();

        Dataset ds = retrieveNeedDataset(needUri);
        if (NeedModelWrapper.isANeed(ds)) {
          StringWriter sw = new StringWriter();
          RDFDataMgr.write(sw, ds, RDFFormat.TRIG.getLang());
          NeedEvent needEvent = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.ACTIVE, crawlDate, sw.toString(),
              RDFFormat.TRIG.getLang());
          bulkNeedEvent.addNeedEvent(needEvent);
        }
      }
      log.debug("number of need events created: " + bulkNeedEvent.getNeedEvents().size());
      return bulkNeedEvent;
    }
  }

}
