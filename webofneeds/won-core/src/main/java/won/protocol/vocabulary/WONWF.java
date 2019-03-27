package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class WONWF {
    public static final String BASE_URI = "http://purl.org/webofneeds/workflow#";
    private static Model m = ModelFactory.createDefaultModel();
    public static Property HAS_INLINE_PETRI_NET_DEFINITION = m.createProperty(BASE_URI + "hasInlinePetriNetDefinition");
    public static Property FIRES_TRANSITION = m.createProperty(BASE_URI + "firesTransition");
}
