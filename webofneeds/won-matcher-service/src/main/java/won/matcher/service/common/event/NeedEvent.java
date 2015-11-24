package won.matcher.service.common.event;


import com.hp.hpl.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import won.protocol.util.RdfUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * This event is used in the matching service to indicate that a new need has been found.
 * It includes the URIs of the need and the won node and optionally the serialized resource
 *
 * User: hfriedrich
 * Date: 04.06.2015
 */
public class NeedEvent implements Serializable
{
  private String uri;
  private String wonNodeUri;
  private String serializedNeedResource;
  private String serializationLangName;
  private String serializationLangContentType;

  private TYPE eventType;

  public static enum TYPE
  {
    CREATED, ACTIVATED, DEACTIVATED
  }

  public NeedEvent(String uri, String wonNodeUri, TYPE eventType, String resource, Lang format) {
    this.uri = uri;
    this.wonNodeUri = wonNodeUri;
    this.eventType = eventType;
    serializedNeedResource = resource;
    serializationLangName = format.getName();
    serializationLangContentType = format.getContentType().getContentType();
  }

  public NeedEvent(String uri, String wonNodeUri, TYPE eventType, Dataset ds) {
    this.uri = uri;
    this.wonNodeUri = wonNodeUri;
    this.eventType = eventType;
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, ds, RDFFormat.TRIG.getLang());
    serializedNeedResource = sw.toString();
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

  public String getSerializedNeedResource() {
    return serializedNeedResource;
  }

  public Lang getSerializationFormat() {
    Lang format = LangBuilder.create(serializationLangName, serializationLangContentType).build();
    return format;
  }

  public Dataset deserializeNeedDataset() throws IOException {
    InputStream is = new ByteArrayInputStream(serializedNeedResource.getBytes(StandardCharsets.UTF_8));
    Lang format = getSerializationFormat();
    Dataset ds = RdfUtils.toDataset(is, new RDFFormat(format));
    is.close();
    return ds;
  }

  @Override
  public NeedEvent clone() {
    NeedEvent e = new NeedEvent(uri, wonNodeUri, eventType, serializedNeedResource, getSerializationFormat());
    return e;
  }

  @Override
  public String toString() {
    return getUri();
  }

}
