package won.matcher.service.common.event;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import won.matcher.service.common.service.sparql.SparqlService;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

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
    private Cause cause;

    public AtomEvent(String uri, String wonNodeUri, TYPE eventType, long crawlDate, String resource, Lang format,
                    Cause cause) {
        this.uri = uri;
        this.wonNodeUri = wonNodeUri;
        this.eventType = eventType;
        this.crawlDate = crawlDate;
        serializedAtomResource = resource;
        serializationLangName = format.getName();
        serializationLangContentType = format.getContentType().getContentTypeStr();
        this.cause = cause;
    }

    public AtomEvent(String uri, String wonNodeUri, TYPE eventType, long crawlDate, Dataset ds, Cause cause) {
        this.uri = uri;
        this.wonNodeUri = wonNodeUri;
        this.eventType = eventType;
        this.crawlDate = crawlDate;
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, ds, RDFFormat.TRIG.getLang());
        serializedAtomResource = sw.toString();
        serializationLangName = RDFFormat.TRIG.getLang().getName();
        serializationLangContentType = RDFFormat.TRIG.getLang().getContentType().getContentTypeStr();
        this.cause = cause;
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

    public Cause getCause() {
        return cause;
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
                        getSerializationFormat(), cause);
        return e;
    }

    @Override
    public String toString() {
        return getUri();
    }

    public static enum TYPE {
        ACTIVE, INACTIVE
    }
}
