package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class WXSCHEMA {
    public static final String BASE_URI = "https://w3id.org/won/ext/schema#";
    public static final String DEFAULT_PREFIX = "wx-schema";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String ReviewSocketString = BASE_URI + "ReviewSocket";
    public static final Resource ReviewSocket = m.createResource(ReviewSocketString);
    public static final String ReviewInverseSocketString = BASE_URI + "ReviewInverseSocket";
    public static final Resource ReviewInverseSocket = m.createResource(ReviewInverseSocketString);
}
