package won.matcher.service.crawler.service;

import org.apache.jena.query.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.service.sparql.SparqlService;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.protocol.vocabulary.WON;

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sparql service extended with methods for crawling
 * <p>
 * User: hfriedrich
 * Date: 04.05.2015
 */
@Component
public class CrawlSparqlService extends SparqlService {
    public static final String METADATA_GRAPH = WON.BASE_URI + "crawlMetadata";
    public static final String CRAWL_DATE_PREDICATE = WON.BASE_URI + "crawlDate";
    public static final String CRAWL_STATUS_PREDICATE = WON.BASE_URI + "crawlStatus";
    public static final String CRAWL_BASE_URI_PREDICATE = WON.BASE_URI + "crawlBaseUri";
    public static final String CRAWL_WON_NODE_URI_PREDICATE = WON.BASE_URI + "wonNodeUri";

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
        String queryString = "prefix won: <http://purl.org/webofneeds/model#>\n" +
                "DELETE WHERE { GRAPH won:crawlMetadata { <" + msg.getUri() + "> ?y ?z}}\n;";

        // insert new entry
        queryString += "\nINSERT DATA { GRAPH won:crawlMetadata {\n" +
                "<" + msg.getUri() + "> won:crawlDate " + msg.getCrawlDate() + ".\n" +
                "<" + msg.getUri() + "> won:crawlStatus '" + msg.getStatus() + "'.\n" +
                "<" + msg.getUri() + "> won:crawlBaseUri <" + msg.getBaseUri() + ">.\n";
        if (msg.getWonNodeUri() != null) {
            queryString += "<" + msg.getUri() + "> won:wonNodeUri <" + msg.getWonNodeUri() + ">.\n";
        }
        queryString += "}}\n";

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
        builder.append("prefix won: <http://purl.org/webofneeds/model#>\n");
        for (CrawlUriMessage msg : msgs) {
            builder.append("\nDELETE WHERE { GRAPH won:crawlMetadata { <");
            builder.append(msg.getUri()).append("> ?p ?o }};");
        }

        // insert the new entries
        builder.append("\n\nINSERT DATA { GRAPH won:crawlMetadata {\n");
        for (CrawlUriMessage msg : msgs) {
            builder.append("<").append(msg.getUri()).append("> won:crawlDate ").append(msg.getCrawlDate()).append(".\n");
            builder.append("<").append(msg.getUri()).append("> won:crawlStatus '").append(msg.getStatus()).append("'.\n");
            builder.append("<").append(msg.getUri()).append("> won:crawlBaseUri <").append(msg.getBaseUri()).append(">.\n");
            if (msg.getWonNodeUri() != null) {
                builder.append("<").append(msg.getUri()).append("> won:wonNodeUri <").append(msg.getWonNodeUri()).append(">.\n");
            }
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
        String queryString = "prefix won: <http://purl.org/webofneeds/model#>\n" +
                "SELECT ?uri ?base ?wonNode WHERE { GRAPH won:crawlMetadata {\n" +
                " ?uri ?p '" + status + "'.\n" +
                " ?uri won:crawlBaseUri ?base.\n" +
                " OPTIONAL { ?uri won:wonNodeUri ?wonNode }\n}}\n";
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
     * Extract linked URIs of resource URI that have not been crawled since a certain date (crawlDateThreshold). Use
     * specified property paths to construct the query.
     *
     * @param uri                current processed resource URI
     * @param properties         property paths used to query the sparql endpoint
     * @param crawlDateThreshold extract only uris that have a crawlDate < crawlDateThreshold
     * @return set of extracted URIs from the resource URI
     */
    public Set<String> extractURIs(String uri, String baseUri, Iterable<String> properties, long crawlDateThreshold) {
        Set<String> extractedURIs = new HashSet<String>();
        for (String prop : properties) {
            if (prop.trim().length() == 0) {
                continue;
                //ignore empty strings
            }
            // select URIs specified by property paths that have not already been crawled
            String queryString = "prefix won: <http://purl.org/webofneeds/model#>\n" +
                    "SELECT ?obj WHERE {\n" +
                    " <" + baseUri + "> " + prop + " ?obj.\n" +
                    " OPTIONAL {?obj won:crawlDate ?crawlDate. }\n" +
                    " FILTER ((?crawlDate < " + crawlDateThreshold + " ) || !BOUND(?crawlDate)) }\n";

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

    /**
     * retrieve the last crawled needUri from a certain won node
     * @param wonNodeUri won node to retrieve the last crawled uri for
     * @return returns the last crawled needUri or null if none exists
     */
    public String retrieveLastCrawledNeedUri(String wonNodeUri) {

        String query = "prefix won: <http://purl.org/webofneeds/model#>\n" +
                "SELECT ?needUri WHERE {\n" +
                " ?needUri a won:Need.\n" +
                " ?needUri won:hasWonNode <" + wonNodeUri + ">. \n" +
                " ?needUri won:crawlDate ?date. \n" +
                "} ORDER BY DESC(?date) LIMIT 1\n";

        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
        ResultSet results = qexec.execSelect();
        String needUri = null;
        if (results.hasNext()) {
            QuerySolution qs = results.nextSolution();
            needUri = qs.get("needUri").asResource().getURI();
        }
        qexec.close();
        return needUri;
    }

    /**
     * retrieve the last crawled connectionUri from a certain won node
     * @param wonNodeUri won node to retrieve the last crawled uri for
     * @return returns the last crawled connectionUri or null if none exists
     */
    public String retrieveLastCrawledConnectionUri(String wonNodeUri) {

        String query = "prefix won: <http://purl.org/webofneeds/model#>\n" +
                "SELECT ?connectionUri WHERE {\n" +
                " ?connectionUri a won:Connection.\n" +
                " ?connectionUri won:hasWonNode <" + wonNodeUri + ">. \n" +
                " ?connectionUri won:crawlDate ?date. \n" +
                "} ORDER BY DESC(?date) LIMIT 1\n";

        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
        ResultSet results = qexec.execSelect();
        String connectionUri = null;
        if (results.hasNext()) {
            QuerySolution qs = results.nextSolution();
            connectionUri = qs.get("connectionUri").asResource().getURI();
        }
        qexec.close();
        return connectionUri;
    }

    public BulkNeedEvent retrieveActiveNeedEvents(long fromDate, long toDate, int offset, int limit, boolean
            sortAscending) {

        // query template to retrieve all alctive cralwed/saved needs in a certain date range
        String orderClause = sortAscending ? "ORDER BY ?date\n" : "ORDER BY DESC(?date)\n";
        log.debug("bulk load need data from sparql endpoint in date range: [{},{}]", fromDate, toDate);
        String queryTemplate = "prefix won: <http://purl.org/webofneeds/model#> \n" +
                "SELECT ?needUri ?wonNodeUri ?date WHERE {  \n" +
                "  ?needUri a won:Need. \n" +
                "  ?needUri won:crawlDate ?date.  \n" +
                "  ?needUri won:isInState won:Active. \n" +
                "  ?needUri won:hasWonNode ?wonNodeUri. \n" +
                "  {?needUri won:crawlStatus 'SAVE'.} UNION {?needUri won:crawlStatus 'DONE'.}\n" +
                "  FILTER (?date >= %d && ?date < %d ) \n" +
                "} " + orderClause +
                " OFFSET %d\n" +
                " LIMIT %d";

        String queryString = String.format(queryTemplate, fromDate, toDate, offset, limit);

        log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
        log.debug("Execute query: {}", queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
        ResultSet results = qexec.execSelect();

        // load all the needs into one bulk need event
        BulkNeedEvent bulkNeedEvent = new BulkNeedEvent();
        while (results.hasNext()) {

            QuerySolution qs = results.nextSolution();
            String needUri = qs.get("needUri").asResource().getURI();
            String wonNodeUri = qs.get("wonNodeUri").asResource().getURI();
            long crawlDate = qs.getLiteral("date").getLong();

            Dataset ds = retrieveNeedDataset(needUri);
            StringWriter sw = new StringWriter();
            RDFDataMgr.write(sw, ds, RDFFormat.TRIG.getLang());
            NeedEvent needEvent = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.CREATED,
                    crawlDate, sw.toString(), RDFFormat.TRIG.getLang());
            bulkNeedEvent.addNeedEvent(needEvent);
        }
        qexec.close();
        log.debug("number of need events created: " + bulkNeedEvent.getNeedEvents().size());
        return bulkNeedEvent;
    }

}
