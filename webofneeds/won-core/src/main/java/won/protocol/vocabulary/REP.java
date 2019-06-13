package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class REP {
    public static final String BASE_URI = "https://w3id.org/won/ext/reputation#";
    public static final String DEFAULT_PREFIX = "srep";
    private static Model m = ModelFactory.createDefaultModel();

    public static final Property ReputationToken = m.createProperty(BASE_URI + "ReputationToken");
    public static final Property blindSignedReputationToken = m.createProperty(BASE_URI + "blindSignedReputationToken");
    public static final Property rating = m.createProperty(BASE_URI + "rating");
    public static final Property ratingComment = m.createProperty(BASE_URI + "ratingComment");
    public static final Property RandomHash = m.createProperty(BASE_URI + "RandomHash");
}
