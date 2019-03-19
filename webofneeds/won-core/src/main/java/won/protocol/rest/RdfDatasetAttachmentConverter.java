package won.protocol.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
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
import won.protocol.vocabulary.WONMSG;

/**
 * User: fsalcher Date: 15.09.2014
 */
public class RdfDatasetAttachmentConverter extends AbstractHttpMessageConverter<Dataset> {
    private static final Logger logger = LoggerFactory.getLogger(RdfDatasetAttachmentConverter.class);

    private static final MediaType[] supportedMediaTypes = { MediaType.ALL // we can do this because we have placed a
                                                                           // hack in the LinkedDataWebController that
            // looks into the dataset and determines the concrete MediaType of the base64-encoded content in the
            // dataset. Thus, spring's content negotiation will choose this class as a compatible converter and the
            // concrete media type as the response media type.
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
    protected Dataset readInternal(Class<? extends Dataset> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Cannot convert arbitrary data to RDF dataset yet.");
    }

    @Override
    protected void writeInternal(Dataset dataset, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        ContentAndMimeType content = RdfUtils.findOne(dataset, new RdfUtils.ModelVisitor<ContentAndMimeType>() {
            @Override
            public ContentAndMimeType visit(Model model) {

                String content = getObjectOfPropertyAsString(model, CNT.BYTES);
                if (content == null)
                    return null;
                String contentType = getObjectOfPropertyAsString(model, WONMSG.CONTENT_TYPE);
                return new ContentAndMimeType(content, contentType);
            }
        }, false);
        if (content.content == null)
            throw new IncorrectPropertyCountException("expected one property cnt:bytes", 1, 0);
        if (content.mimeType == null)
            throw new IncorrectPropertyCountException("expected one property msg:contentType", 1, 0);
        // TODO: here, we decode the base64 content only to have it encoded again by the framework. Can we tell the
        // webMVC framework not to encode in base64 somehow?
        httpOutputMessage.getHeaders().setContentType(MediaType.valueOf(content.mimeType));
        OutputStream body = httpOutputMessage.getBody();
        body.write(Base64.getDecoder().decode(content.content));
        body.flush();
    }

    private String getObjectOfPropertyAsString(Model model, Property property) {
        NodeIterator nodeIteratr = model.listObjectsOfProperty(property);
        if (!nodeIteratr.hasNext())
            return null;
        String ret = nodeIteratr.next().asLiteral().getString();
        if (nodeIteratr.hasNext()) {
            throw new IncorrectPropertyCountException("found more than one property of cnt:bytes", 1, 2);
        }
        return ret;
    }

    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(supportedMediaTypes);
    }

    private class ContentAndMimeType {
        public String content;
        public String mimeType;

        public ContentAndMimeType(final String content, final String mimeType) {
            this.content = content;
            this.mimeType = mimeType;
        }
    }
}
