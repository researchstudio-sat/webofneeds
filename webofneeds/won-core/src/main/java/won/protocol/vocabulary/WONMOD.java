package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class WONMOD {
    public static final String BASE_URI = "https://w3id.org/won/modification#";
    private static Model m = ModelFactory.createDefaultModel();
    public static Property retracts = m.createProperty(BASE_URI + "retracts");
}
