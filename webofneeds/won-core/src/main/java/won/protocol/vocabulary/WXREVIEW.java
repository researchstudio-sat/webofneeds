package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class WXREVIEW {
    public static final String BASE_URI = "https://w3id.org/won/ext/review#";
    public static final String DEFAULT_PREFIX = "review";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String ReviewSocketString = BASE_URI + "ReviewSocket";
    public static final Resource ReviewSocket = m.createResource(ReviewSocketString);
    public static final String reviewsString = BASE_URI + "reviews";
    public static final Property reviewedConnection = m.createProperty(BASE_URI + "reviewedConnection");
    public static final Resource reviews = m.createResource(reviewsString);
}
