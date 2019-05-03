package won.matcher.service.common.event;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import won.matcher.service.common.mailbox.PriorityAtomEventMailbox;
import won.matcher.service.common.service.sparql.SparqlService;

/**
 * This event is used in the matching service to indicate that a new atom has
 * been found. It includes the URIs of the atom and the won node and optionally
 * the serialized resource User: hfriedrich Date: 04.06.2015
 */
public class AtomEvent implements Serializable {
    private String uri;
    private String wonNodeUri;
    private String serializedAtomResource;
    private String serializationLangName;
    private String serializationLangContentType;
    private long crawlDate;
    private TYPE eventType;
    private Priority priority;

    /**
     * Indicates the priority this event should be given by matchers. The priority
     * should be given based on how the event was generated. Used in the
     * {@link PriorityAtomEventMailbox}; lower priority values means more important.
     */
    public static enum Priority {
        PUSHED(0), // we received a push about this Atom from a WoN node. React fast!
        SCHEDULED_FOR_REMATCH(5), // we decided it's time to re-match for this Atom. Should be handled before
                                  // events generated in crawling
        CRAWLED(10), // we found this Atom during crawling. Handle when we have nothing else to do
        ;
        private int priority;

        private Priority(int prio) {
            this.priority = prio;
        }

        public int getPriority() {
            return priority;
        }

        public static int LOWEST_PRIORTY = Integer.MAX_VALUE;
    }

    public static enum TYPE {
        ACTIVE, INACTIVE
    }

    public AtomEvent(String uri, String wonNodeUri, TYPE eventType, long crawlDate, String resource, Lang format,
                    Priority priority) {
        this.uri = uri;
        this.wonNodeUri = wonNodeUri;
        this.eventType = eventType;
        this.crawlDate = crawlDate;
        serializedAtomResource = resource;
        serializationLangName = format.getName();
        serializationLangContentType = format.getContentType().getContentType();
    }

    public AtomEvent(String uri, String wonNodeUri, TYPE eventType, long crawlDate, Dataset ds, Priority priority) {
        this.uri = uri;
        this.wonNodeUri = wonNodeUri;
        this.eventType = eventType;
        this.crawlDate = crawlDate;
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, ds, RDFFormat.TRIG.getLang());
        serializedAtomResource = sw.toString();
        serializationLangName = RDFFormat.TRIG.getLang().getName();
        serializationLangContentType = RDFFormat.TRIG.getLang().getContentType().getContentType();
    }

    public String getUri() {
        return uri;
    }

    public String getWonNodeUri() {
        return wonNodeUri;
    }

    public TYPE getEventType() {
        return eventType;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getSerializedAtomResource() {
        return serializedAtomResource;
    }

    public Lang getSerializationFormat() {
        Lang format = LangBuilder.create(serializationLangName, serializationLangContentType).build();
        return format;
    }

    public long getCrawlDate() {
        return crawlDate;
    }

    public Dataset deserializeAtomDataset() throws IOException {
        return SparqlService.deserializeDataset(serializedAtomResource, getSerializationFormat());
    }

    @Override
    public AtomEvent clone() {
        AtomEvent e = new AtomEvent(uri, wonNodeUri, eventType, crawlDate, serializedAtomResource,
                        getSerializationFormat(), priority);
        return e;
    }

    @Override
    public String toString() {
        return getUri();
    }
}
