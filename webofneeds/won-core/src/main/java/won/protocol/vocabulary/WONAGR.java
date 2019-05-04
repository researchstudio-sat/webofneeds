package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class WONAGR {
    public static final String BASE_URI = "https://w3id.org/won/agreement#";
    private static Model m = ModelFactory.createDefaultModel();
    public static Property proposes = m.createProperty(BASE_URI + "proposes");
    public static Property proposesToCancel = m.createProperty(BASE_URI + "proposesToCancel");
    public static Property accepts = m.createProperty(BASE_URI + "accepts");
    public static Property rejects = m.createProperty(BASE_URI + "rejects");
    public static Property claims = m.createProperty(BASE_URI + "claims");
}
