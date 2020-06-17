package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class WONCON {
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
    public static final Resource ServiceBot = m.createProperty(BASE_URI + "ServiceBot");
    public static final Property feedbackEvent = m.createProperty(BASE_URI, "feedbackEvent");
    public static final Property feedback = m.createProperty(BASE_URI, "feedback");
    public static final Property feedbackTarget = m.createProperty(BASE_URI, "feedbackTarget");
    public static final Property binaryRating = m.createProperty(BASE_URI, "binaryRating");
    public static final Resource Good = m.createResource(BASE_URI + "Good");
    public static final Resource Bad = m.createResource(BASE_URI + "Bad");
    public static final Property isProcessing = m.createProperty(BASE_URI + "isProcessing");
    public static final Property boundingBox = m.createProperty(BASE_URI, "boundingBox");
    public static final Property northWestCorner = m.createProperty(BASE_URI, "northWestCorner");
    public static final Property southEastCorner = m.createProperty(BASE_URI, "southEastCorner");
    public static final Property geoSpatial = m.createProperty(BASE_URI + "geoSpatial");

    /**
     * Returns the base URI for this schema.
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
