package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class WONCNT {
    public static final String BASE_URI = "https://w3id.org/won/content#";
    public static final String DEFAULT_PREFIX = "con";
    private final static Model m = ModelFactory.createDefaultModel();
    public static final Property travelAction = m.createProperty(BASE_URI + "travelAction");
    public static final Property inResponseTo = m.createProperty(BASE_URI + "inResponseTo");
    public static final Property suggestedAtom = m.createProperty(BASE_URI + "suggestedAtom");
    public static final Property bpmnWorkflow = m.createProperty(BASE_URI + "bpmnWorkflow");
    public static final Property file = m.createProperty(BASE_URI + "file");
    public static final Property image = m.createProperty(BASE_URI + "image");
    public static final Property petriNet = m.createProperty(BASE_URI + "petriNet");
    public static final Property tag = m.createProperty(BASE_URI + "tag");
    public static final Property text = m.createProperty(BASE_URI + "text");
    public static final Resource DirectResponse = m.createProperty(BASE_URI + "DirectResponse");
    public static final Resource ServiceBot = m.createProperty(BASE_URI + "ServiceBot");
    
    // used to express which URI the feedback relates to
    public static final Property feedback = m.createProperty(BASE_URI, "feedback");
    public static final Property feedbackEvent = m.createProperty(BASE_URI, "feedbackEvent");
    public static final Property forResource = m.createProperty(BASE_URI, "forResource");
    public static final Property binaryRating = m.createProperty(BASE_URI, "binaryRating");
    public static final Resource Good = m.createResource(BASE_URI + "Good");
    public static final Resource Bad = m.createResource(BASE_URI + "Bad");
    
    public static final Property isProcessing = m.createProperty(BASE_URI + "isProcessing");
}
