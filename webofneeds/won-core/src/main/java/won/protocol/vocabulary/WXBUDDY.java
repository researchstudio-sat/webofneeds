package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class WXBUDDY {
    public static final String BASE_URI = "https://w3id.org/won/ext/buddy#";
    public static final String DEFAULT_PREFIX = "bdy";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String BuddySocketString = BASE_URI + "BuddySocket";
    public static final Resource BuddySocket = m.createResource(BuddySocketString);
    public static final String buddyString = BASE_URI + "buddy";
    public static final Resource buddy = m.createResource(buddyString);
}
