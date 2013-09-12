package won.matcher.webapp;

import com.github.jsonldjava.core.JSONLD;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.codehaus.jackson.map.JsonSerializableWithType
import org.springframework.http.converter.HttpMessageNotWritableException;


import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: moru
 * Date: 11/09/13
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
//public class RdfModelConverter {
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

        httpInputMessage.getHeaders().getAccept(); //media types

        Model model = ModelFactory.createDefaultModel();

        //TODO How can this not throw exceptions? does it return an empty model if it can't parse the query?
        RDFDataMgr.read(model, httpInputMessage.getBody(), Lang.RDFJSON); //TODO use json-ld


        return model;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void writeInternal(Model model, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        //RDFDataMgr.w
        //throw new HttpMessageNotWritableException();
        //RDFDataMgr.

        //JSONLD.fromRDF
        //JSONLD.toRDF

        for(MediaType t : httpOutputMessage.getHeaders().getAccept()) { //media types
            System.out.println(t); //empty here
        }
        System.out.println(httpOutputMessage.getHeaders().getContentType()); //application/ld+json, (register converter for all types, use this method to determine what the user wants)

        //JenaModel to json-ld rdfdataset
        // triple by triple (quads to jena-statements)
        // dataset can have several graphs (as they use quads) --> name clashes
        // or: if multiple graphs -> exception
        //context for governance (my data) / to distinguish on clashes /...

        //RDFDataMgr.write(httpOutputMessage.getBody(), model, Lang.RDFNULL);
        model.write(httpOutputMessage.getBody(), "JSON-LD");
    }
}
