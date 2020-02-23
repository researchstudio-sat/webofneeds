package won.matcher.service.rematch.service;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.event.AtomEvent.TYPE;
import won.matcher.service.common.event.BulkAtomEvent;
import won.matcher.service.common.event.Cause;
import won.matcher.service.common.service.sparql.SparqlService;
import won.matcher.service.crawler.config.CrawlConfig;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * Sparql service extended with methods for rematching
 * <p>
 * User: hfriedrich Date: 04.05.2015
 */
@Component
public class RematchSparqlService extends SparqlService {
    int MAX_ATOMS_PER_REMATCH_BULK = 10;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String HTTP_HEADER_SEPARATOR = ", ";
    @Autowired
    LinkedDataSource linkedDataSource;

    @Autowired
    public RematchSparqlService(@Value("${uri.sparql.endpoint}") final String sparqlEndpoint) {
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
    public void registerMatchingAttempt(AtomEvent msg) {
        createMatchAttemptUpdate(msg).ifPresent(updateString -> executeUpdateQuery(updateString));
    }

    /**
     * Bulk update of several meta data messages about the crawling process using a
     * separate graph.
     * 
     * @param msg multiple messages that describe crawling meta data to update
     */
    public void registerMatchingAttempts(BulkAtomEvent msg) {
        final AtomicInteger counter = new AtomicInteger();
        StringBuilder builder = new StringBuilder();
        msg.getAtomEvents()
                        .stream()
                        .map(e -> createMatchAttemptUpdate(e))
                        .filter(o -> o.isPresent())
                        .map(o -> o.get())
                        .map(updateString -> parseUpdateQuery(updateString))
                        .filter(o -> o.isPresent())
                        .map(o -> o.get())
                        // collect the UpdateRequests into bins of size MAX_UPDATES_PER_REQUEST
                        .collect(Collectors.groupingBy(x -> counter.getAndIncrement() % MAX_UPDATES_PER_REQUEST))
                        .values()
                        .stream() // stream of list of updateRequest
                        .forEach(requests -> {
                            requests.stream()
                                            // reduce the list of UpdateRequest into one UpdateRequest
                                            .reduce((UpdateRequest left, UpdateRequest right) -> {
                                                right.getOperations().stream().forEach(left::add);
                                                return left;
                                            })
                                            .ifPresent(request -> executeUpdate(request));
                        });
    }

    private Optional<String> createMatchAttemptUpdate(AtomEvent msg) {
        if (msg.getEventType() == TYPE.INACTIVE) {
            return Optional.empty();
        }
        // insert a new entry,
        StringBuilder builder = new StringBuilder();
        // in case of a new push from the WoN node, remove the reference
        // date for this atom - it will be added by the subsequent insert
        if (msg.getCause() == Cause.PUSHED) {
            builder.append(" DELETE {  \n");
            builder.append("  graph won:rematchMetadata {  \n");
            builder.append("    ?atomUri won:referenceDate ?refDate ; \n");
            builder.append("  } \n");
            builder.append(" } \n");
            builder.append(" WHERE {  \n");
            builder.append("  graph won:rematchMetadata {  \n");
            builder.append("    ?atomUri won:referenceDate ?refDate . \n");
            builder.append("  } \n");
            builder.append(" }; \n");
        }
        // insert a new entry, using the current date as matchAttemptDate and
        // referenceDate
        // (the latter will be removed afterwards if it is not the earliest one)
        builder.append(" INSERT DATA {  \n");
        builder.append("  graph won:rematchMetadata { \n");
        builder.append("      ?atomUri won:matchAttemptDate ?matchAttemptDate; \n");
        builder.append("                  won:referenceDate ?referenceDate. \n");
        builder.append("           \n");
        builder.append("     } \n");
        builder.append(" }; \n");
        // this DELETE/INSERT WHERE update ensures that
        // * only the latest matchAttemptDate is kept
        // * only the earliest referenceDate is kept
        builder.append(" DELETE {  \n");
        builder.append("  graph won:rematchMetadata {  \n");
        builder.append("    ?atomUri won:matchAttemptDate ?olderMAD ; \n");
        builder.append("         won:referenceDate ?newerRD ; \n");
        builder.append("  } \n");
        builder.append(" } \n");
        builder.append(" WHERE  \n");
        builder.append(" {  \n");
        builder.append("  graph won:rematchMetadata {  \n");
        builder.append("    ?atomUri won:matchAttemptDate ?newerMAD; \n");
        builder.append("         won:referenceDate ?olderRD . \n");
        builder.append("    optional { \n");
        builder.append("      ?atomUri won:matchAttemptDate ?olderMAD  \n");
        builder.append("      filter (?olderMAD < ?newerMAD) \n");
        builder.append("    } \n");
        builder.append("    optional { \n");
        builder.append("      ?atomUri won:referenceDate ?newerRD \n");
        builder.append("      filter (?olderRD < ?newerRD) \n");
        builder.append("    } \n");
        builder.append("    filter (bound(?newerRD) && bound (?olderMAD)) \n");
        builder.append("  } \n");
        builder.append(" }; \n");
        builder.append("  \n");
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(builder.toString());
        pss.setNsPrefix("won", "https://w3id.org/won/core#");
        pss.setIri("atomUri", msg.getUri());
        long now = System.currentTimeMillis();
        pss.setLiteral("matchAttemptDate", now);
        pss.setLiteral("referenceDate", now);
        return Optional.of(pss.toString());
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

    public Set<BulkAtomEvent> findAtomsForRematching() {
        logger.debug("searching atoms for rematching");
        StringBuilder builder = new StringBuilder();
        // Selects atomUris using a back-off strategy, each time doubling
        // the time difference to the reference date
        builder.append(" prefix won: <https://w3id.org/won/core#> \n");
        builder.append(" select distinct ?atomUri where {  \n");
        builder.append("    graph won:rematchMetadata { \n");
        builder.append("        ?atomUri won:referenceDate ?rDate ; \n");
        builder.append("                  won:matchAttemptDate ?mDate . \n");
        builder.append("         filter (?mDate >= ?rDate) \n");
        builder.append("         bind (?mDate - ?rDate as ?lastDiff) \n");
        builder.append("         bind (?now - ?rDate as ?diff) \n");
        builder.append("     } \n");
        builder.append("  \n");
        builder.append("     filter(?diff > 2 * ?lastDiff) \n");
        builder.append(" } \n");
        ParameterizedSparqlString pps = new ParameterizedSparqlString();
        pps.setNsPrefix("won", "https://w3id.org/won/core#");
        pps.setCommandText(builder.toString());
        pps.setLiteral("now", System.currentTimeMillis());
        Set<BulkAtomEvent> bulks = new HashSet<>();
        BulkAtomEvent bulkAtomEvent = new BulkAtomEvent();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, pps.asQuery())) {
            ResultSet results = qexec.execSelect();
            // load all the atoms into one bulk atom event
            int i = 0;
            while (results.hasNext()) {
                QuerySolution qs = results.nextSolution();
                String atomUri = qs.get("atomUri").asResource().getURI();
                Dataset ds = linkedDataSource.getDataForResource(URI.create(atomUri));
                if (AtomModelWrapper.isAAtom(ds)) {
                    StringWriter sw = new StringWriter();
                    RDFDataMgr.write(sw, ds, RDFFormat.TRIG.getLang());
                    AtomEvent atomEvent = new AtomEvent(atomUri, null, AtomEvent.TYPE.ACTIVE,
                                    System.currentTimeMillis(), sw.toString(), RDFFormat.TRIG.getLang(),
                                    Cause.SCHEDULED_FOR_REMATCH);
                    bulkAtomEvent.addAtomEvent(atomEvent);
                    if (++i >= MAX_ATOMS_PER_REMATCH_BULK) {
                        bulks.add(bulkAtomEvent);
                        bulkAtomEvent = new BulkAtomEvent();
                    }
                }
            }
        }
        logger.debug("atomEvents for rematching: " + bulkAtomEvent.getAtomEvents().size());
        return bulks;
    }

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }
}
