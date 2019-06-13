package won.protocol.util;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WONCON;

public class WonRepRdfUtils extends WonRdfUtils {
    private static final Logger logger = LoggerFactory.getLogger(WonRepRdfUtils.class);

    public static Model addStuff(Model model) {
//        Model messageModel = createModelWithBaseResource();
//        Resource baseRes = messageModel.createResource(messageModel.getNsPrefixURI(""));
//        baseRes.addProperty(WONCON.text, message, XSDDatatype.XSDstring);
//        return messageModel;
        return null;
    }

    public static Model createBaseModel() {
        return createModelWithBaseResource();
    }

    private static Model createModelWithBaseResource() {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("", "no:uri");
        model.createResource(model.getNsPrefixURI(""));
        return model;
    }
}
