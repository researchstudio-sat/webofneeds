package won.matcher.webapp;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.impl.TurtleRDFParser;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
        System.out.println("Input: ");
        for(MediaType t : httpInputMessage.getHeaders().getAccept()) { //media types
            System.out.println(t); //empty here
        }
        System.out.println(httpInputMessage.getHeaders().getContentType()); //application/ld+json, (register converter for all types, use this method to determine what the user wants)


        Model model = ModelFactory.createDefaultModel();
        String jenaType = mimeTypeToJenaLanguage(httpInputMessage.getHeaders().getContentType(), null);


        //TODO How can this not throw exceptions? does it return an empty model if it can't parse the query?

        return model.read(httpInputMessage.getBody(), jenaType);
    }

    @Override
    protected void writeInternal(Model model, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        //RDFDataMgr.w
        //throw new HttpMessageNotWritableException();
        //RDFDataMgr.

        //JSONLD.fromRDF
        //JSONLD.toRDF


        System.out.println("Output: ");
        System.out.println("1: " + httpOutputMessage.getHeaders().getContentType().getType());
        System.out.println("2: " + httpOutputMessage.getHeaders().getContentType().getSubtype());
        for(MediaType t : httpOutputMessage.getHeaders().getAccept()) { //media types
            System.out.println(t); //empty here
        }
        System.out.println(httpOutputMessage.getHeaders().getContentType()); //application/ld+json, (register converter for all types, use this method to determine what the user wants)

        jenaToJsonLd(model);


        //TODO
        //JenaModel to json-ld rdfdataset
        // triple by triple (quads to jena-statements)
        // dataset can have several graphs (as they use quads) --> name clashes
        // or: if multiple graphs -> exception
        //context for governance (my data) / to distinguish on clashes /...

        //RDFDataMgr.write(httpOutputMessage.getBody(), model, Lang.RDFNULL);
        //if(httpOutputMessage.getHeaders().getContentType().getType());
        String jenaType = mimeTypeToJenaLanguage(httpOutputMessage.getHeaders().getContentType(), "N3");

        if(jenaType.equals("JSON-LD")) {
            //TODO convert between jsonld and jena here
            model.listStatements();
            //model.
            //JSONLD.

        } else {
            model.write(httpOutputMessage.getBody(), jenaType);
        }
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
        //TODO: is this correct? Check Media Type specs!
        if (type == null || subtype == null) return defaultLanguage;
        return type + "/" + subtype;
    }

    public static void jenaToJsonLd(Model model) {
        System.out.println("Jena to jsonld:");
        RDFDataset dataSet = new RDFDataset();
        Statement stmt = null;
        for(StmtIterator iter = model.listStatements(); iter.hasNext(); stmt = iter.next()) {
            if(stmt != null) {
                System.out.println(stmt.asTriple().getSubject().toString());
                System.out.println(stmt);

                Triple t = stmt.asTriple();
                String s = t.getSubject().toString();
                String p = t.getPredicate().toString();
                String o = t.getObject().toString();

                dataSet.addTriple(s, p, o); //=addQuad with context = "@default"
                //TODO make sure we're only working with a single graph here (throw an exception on clashes?)
                //TODO How did they format the code in actual json-ld syntax on their hp instad of just getting linkedHashMap.toString()
            }
        }

        System.out.println(dataSet.toString()); //TODO uargh, that format
        System.out.println(JSONUtils.toPrettyString(dataSet));


        //via TTL-Strings
        //RDFDataset dataSet2 = new RDFDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        model.write(baos);
        String ttlString = null; //sure this requires a string?
        try {
            ttlString = new String(baos.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        /*try {
            dataSet2 = (RDFDataset)JSONLD.fromRDF(ttlString, new TurtleRDFParser()); //throws com.github.jsonldjava.core.JSONLDProcessingError: Error while parsing Turtle; missing expected subject. {position:0} {line:1}
        } catch (JSONLDProcessingError jsonldProcessingError) {
            jsonldProcessingError.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println(ttlString);
        System.out.println(dataSet2);*/


    }

    /*
    public static class MimeT {
        public static String RDF_XML = "application/rdf+xml";
        public static String X_TURTLE = "application/x-turtle";
        public static String JSON_LD = "application/ld+json";
        public static String PLAIN = "text/plain";
        public static String TURTLE = "text/turtle";
        public static String RDF_N3 = "text/rdf+n3";
    }

    public static class JenaT {
        public static String RDF_XML = "RDF/XML";
        public static String TTL = "TTL";
        public static String JSON_LD = "JSON-LD";
        public static String N3 = "N3";

    }

    private static String mimeTypeToJenaLanguage(MediaType mediaType, String defaultLanguage) {
        String mimeType = combinedMimeType(mediaType, defaultLanguage);
        if (MimeT.RDF_XML.equalsIgnoreCase(mimeType)) return JenaT.RDF_XML;
        if (MimeT.X_TURTLE.equalsIgnoreCase(mimeType)) return JenaT.TTL;
        if (MimeT.JSON_LD.equalsIgnoreCase(mimeType)) return JenaT.JSON_LD;
        if (MimeT.PLAIN.equalsIgnoreCase(mimeType)) return JenaT.N3;
        if (MimeT.TURTLE.equalsIgnoreCase(mimeType)) return JenaT.TTL;
        if (MimeT.RDF_N3.equalsIgnoreCase(mimeType)) return JenaT.N3;
        return defaultLanguage;
    }
    */
}
