package won.matcher.webapp;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import jenajsonld.JsonLDReader;
import jenajsonld.JsonLDWriter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;

//import org.codehaus.jackson.map.JsonSerializableWithType

/**
 * Created with IntelliJ IDEA.
 * User: moru
 * Date: 11/09/13
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
public class RdfModelConverter extends AbstractHttpMessageConverter<Model> {

    public RdfModelConverter() {
    }

    public RdfModelConverter(MediaType supportedMediaType) {
        super(supportedMediaType);
    }

    public RdfModelConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }
    @Override
    protected boolean supports(Class<?> clazz) {
        return Model.class.isAssignableFrom(clazz);
    }

    @Override
    protected Model readInternal(Class<? extends Model> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        System.out.println("Input: ");
        for(MediaType t : httpInputMessage.getHeaders().getAccept()) { //media types
            System.out.println(t); //empty here
        }
        System.out.println(httpInputMessage.getHeaders().getContentType()); //application/ld+json, (register converter for all types, use this method to determine what the user wants)
        Model model = ModelFactory.createDefaultModel();
        String jenaType = mimeTypeToJenaLanguage(httpInputMessage.getHeaders().getContentType(), FileUtils.langTurtle);
        if (jenaType.equals("JSON-LD")){
          JsonLDReader reader = new JsonLDReader();
          return reader.read(httpInputMessage.getBody(), "").getDefaultModel();
        }  else {
          return model.read(httpInputMessage.getBody(), jenaType);
        }
    }

    @Override
    protected void writeInternal(Model model, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        String jenaType = mimeTypeToJenaLanguage(httpOutputMessage.getHeaders().getContentType(), "N3");

        if(jenaType.equals("JSON-LD")) {
            JsonLDWriter writer = new JsonLDWriter(true);
            writer.write(httpOutputMessage.getBody(), model.getGraph(), model.getNsPrefixURI(""));
        } else {
            model.write(httpOutputMessage.getBody(), jenaType);
        }
      httpOutputMessage.getBody().flush();
    }

    private static String mimeTypeToJenaLanguage(MediaType mediaType, String defaultLanguage) {
        String mimeType = combinedMimeType(mediaType, defaultLanguage);
        if ("application/rdf+xml".equalsIgnoreCase(mimeType)) return "RDF/XML";
        if ("application/x-turtle".equalsIgnoreCase(mimeType)) return "TTL";
        if ("application/ld+json".equalsIgnoreCase(mimeType)) return "JSON-LD";
        if ("text/plain".equalsIgnoreCase(mimeType)) return "N3";
        if ("text/turtle".equalsIgnoreCase(mimeType)) return "TTL";
        if ("text/rdf+n3".equalsIgnoreCase(mimeType)) return "N3";
        return defaultLanguage;
    }

    private static String combinedMimeType(MediaType mediaType, String defaultLanguage) {
        String type = mediaType.getType();
        String subtype = mediaType.getSubtype();
        if (type == null || subtype == null) return defaultLanguage;
        return type + "/" + subtype;
    }

}
