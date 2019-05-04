package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class WONWF {
    public static final String BASE_URI = "https://w3id.org/won/workflow#";
    private static Model m = ModelFactory.createDefaultModel();
    public static Property inlinePetriNetDefinition = m.createProperty(BASE_URI + "inlinePetriNetDefinition");
    public static Property firesTransition = m.createProperty(BASE_URI + "firesTransition");
}
