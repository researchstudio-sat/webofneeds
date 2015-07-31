package won.protocol.rest;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.CNT;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * User: fsalcher
 * Date: 15.09.2014
 */
public class RdfDatasetAttachmentConverter extends AbstractHttpMessageConverter<Dataset>
{
  private static final Logger logger = LoggerFactory.getLogger(RdfDatasetAttachmentConverter.class);

  private static final MediaType[] supportedMediaTypes =  {
    MediaType.IMAGE_PNG
  };


  public RdfDatasetAttachmentConverter() {
    this(supportedMediaTypes);
  }

  public RdfDatasetAttachmentConverter(MediaType supportedMediaType) {
    super(supportedMediaType);
  }

  public RdfDatasetAttachmentConverter(MediaType... supportedMediaTypes) {
    super(supportedMediaTypes);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return Dataset.class.isAssignableFrom(clazz);
  }

  @Override
  protected Dataset readInternal(Class<? extends Dataset> aClass, HttpInputMessage httpInputMessage) throws IOException,
    HttpMessageNotReadableException {
    throw new UnsupportedOperationException("Cannot convert arbitrary data to RDF dataset yet.");
  }

  @Override
  protected void writeInternal(Dataset dataset, HttpOutputMessage httpOutputMessage) throws IOException,
    HttpMessageNotWritableException {
    String content = RdfUtils.findOne(dataset, new RdfUtils.ModelVisitor<String>() {
      @Override
      public String visit(Model model) {
        NodeIterator nodeIteratr = model.listObjectsOfProperty(CNT.BYTES);
        if (!nodeIteratr.hasNext()) return null;
        String content = nodeIteratr.next().asLiteral().getString();
        if (nodeIteratr.hasNext()) {
          throw new IncorrectPropertyCountException("found more than one property of cnt:bytes", 1, 2);
        }
        return content;
      }
    }, false);
    //TODO: here, we decode the base64 content only to have it encoded again by the framework. Can we tell the webMVC framework not to encode in base64 somehow?
    httpOutputMessage.getBody().write(Base64.getDecoder().decode(content));
    httpOutputMessage.getBody().flush();
  }

  private static Lang mimeTypeToJenaLanguage(MediaType mediaType, Lang defaultLanguage) {
    Lang lang = RDFLanguages.contentTypeToLang(mediaType.toString());
    if (lang == null) return defaultLanguage;
    return lang;
  }

  public List<MediaType> getSupportedMediaTypes(){
      return Arrays.asList(supportedMediaTypes);
  }
}
