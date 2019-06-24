package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class REP {
    public static final String BASE_URI = "https://w3id.org/won/ext/reputation#";
    public static final String DEFAULT_PREFIX = "srep";
    private static Model m = ModelFactory.createDefaultModel();

    public static final Property CERTIFICATE = m.createProperty(BASE_URI + " certificate");
    public static final Property USER_ID = m.createProperty(BASE_URI + " userId");
    public static final Property PUBLIC_KEY = m.createProperty(BASE_URI + " publicKey");
    public static final Property REPUTATIONTOKEN = m.createProperty(BASE_URI + "ReputationToken");
    public static final Property BLIND_SIGNED_REPUTATIONTOKEN = m.createProperty(BASE_URI + "blindSignedReputationToken");
    public static final Property RATING = m.createProperty(BASE_URI + "rating");
    public static final Property RATING_COMMENT = m.createProperty(BASE_URI + "ratingComment");
    public static final Property RANDOM_HASH = m.createProperty(BASE_URI + "RandomHash");
    public static final Property SIGNED_RANDOM_HASH = m.createProperty(BASE_URI + "signedRandomHash");
    public static final Property VERIFICATION_STATE = m.createProperty(BASE_URI + "verificationState");

}
