package won.node.rest;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Properties;

/**
 * Writer for rdf graph serialization.
 */
@Provider
@Produces("text/html,application/rdf+xml,text/plain,application/x-turtle,text/turtle,text/rdf+n3,application/json")
@Consumes("application/rdf+xml,text/plain,application/x-turtle,text/turtle,text/rdf+n3,application/json")
public class ModelReaderWriter implements MessageBodyWriter<Model>, MessageBodyReader<Model>
{

  private final Logger logger = LoggerFactory.getLogger(getClass());
  protected Properties serverProperties;
  private String BASE_URI = "http://example.com/resource";
  private static final String DEFAULT_RDF_LANGUAGE = "TURTLE";

  public ModelReaderWriter()
  {
  }


  @Override
  public long getSize(Model t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
  {
    return -1;
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
  {
    return Model.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(Model model, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
  {
    if (logger.isTraceEnabled()) {
      logger.trace("writeTo called on GraphWriter, mediaType=" + mediaType);
    }
    try {
      model.write(entityStream,mimeTypeToJenaLanguage(mediaType, DEFAULT_RDF_LANGUAGE));
    } finally {
      entityStream.flush();
    }
  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
  {
//        return Graph.class.isAssignableFrom(type);
    return true;
  }

  @Override
  public Model readFrom(Class<Model> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
  {
    Model ret = ModelFactory.createDefaultModel();
    logger.trace("readFrom called on GraphWriter, mediaType=" + mediaType);
    ret.read(entityStream,mimeTypeToJenaLanguage(mediaType, DEFAULT_RDF_LANGUAGE));
    return ret;
  }

  private String mimeTypeToJenaLanguage(MediaType mediaType, String defaultLanguage) {
    String type = mediaType.getType();
    String subtype = mediaType.getSubtype();
    //TODO: is this correct? Check Media Type specs!
    if (type == null || subtype == null) return defaultLanguage;
    String mimeType = type + "/" + subtype;
    if ("application/rdf+xml".equalsIgnoreCase(mimeType)) return "RDF/XML";
    if ("application/x-turtle".equalsIgnoreCase(mimeType)) return "TURTLE";
    if ("text/plain".equalsIgnoreCase(mimeType)) return "N3";
    if ("text/turtle".equalsIgnoreCase(mimeType)) return "TURTLE";
    if ("text/rdf+n3".equalsIgnoreCase(mimeType)) return "N3";
    return defaultLanguage;
  }

}
