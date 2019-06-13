package won.protocol.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StopWatch;

import won.protocol.util.RdfUtils;

/**
 * Converts between Jena RDF datasets and HTTP responses (server side and client
 * side).
 * <p>
 * In the context of WoN, this is configured in LinkedDataRestClientHttps for
 * the client and in linkedDataPageServlet-servlet.xml (won-node-webapp) and
 * dispatcherServlet.xml (won-owner-webapp).
 * </p>
 */
public class RdfDatasetConverter extends AbstractHttpMessageConverter<Dataset> {
    private static final Logger logger = LoggerFactory.getLogger(RdfDatasetConverter.class);
    private static final MediaType[] supportedMediaTypes = {
                    RDFMediaType.APPLICATION_TRIG,
                    RDFMediaType.APPLICATION_NQUADS,
                    RDFMediaType.APPLICATION_X_TURTLE,
                    RDFMediaType.TEXT_RDF_N3,
                    RDFMediaType.TEXT_TURTLE,
                    RDFMediaType.APPLICATION_JSONLD };

    public RdfDatasetConverter() {
        this(supportedMediaTypes);
    }

    public RdfDatasetConverter(MediaType supportedMediaType) {
        super(supportedMediaType);
    }

    public RdfDatasetConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Dataset.class.isAssignableFrom(clazz);
    }

    @Override
    protected Dataset readInternal(Class<? extends Dataset> aClass, HttpInputMessage httpInputMessage)
                    throws IOException, HttpMessageNotReadableException {
        Lang rdfLanguage = mimeTypeToJenaLanguage(httpInputMessage.getHeaders().getContentType(), Lang.TRIG);
        return RdfUtils.toDataset(httpInputMessage.getBody(), new RDFFormat(rdfLanguage));
    }

    @Override
    protected void writeInternal(Dataset dataset, HttpOutputMessage httpOutputMessage)
                    throws IOException, HttpMessageNotWritableException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        MediaType contentType = httpOutputMessage.getHeaders().getContentType();
        Lang rdfLanguage = mimeTypeToJenaLanguage(contentType, Lang.TRIG);
        WonEtagHelper.setMediaTypeForEtagHeaderIfPresent(contentType, httpOutputMessage.getHeaders());
        RDFDataMgr.write(httpOutputMessage.getBody(), dataset, rdfLanguage);
        // append content type to ETAG header to avoid confusing different
        // representations of the same resource
        httpOutputMessage.getBody().flush();
        stopWatch.stop();
        logger.debug("writing dataset took " + stopWatch.getLastTaskTimeMillis() + " millls");
    }

    private static Lang mimeTypeToJenaLanguage(MediaType mediaType, Lang defaultLanguage) {
        Lang lang = RDFLanguages.contentTypeToLang(mediaType.toString());
        if (lang == null)
            return defaultLanguage;
        return lang;
    }

    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(supportedMediaTypes);
    }
}
