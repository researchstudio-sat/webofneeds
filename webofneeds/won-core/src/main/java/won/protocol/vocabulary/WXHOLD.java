package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class WXHOLD {
    public static final String BASE_URI = "https://w3id.org/won/ext/hold#";
    public static final String DEFAULT_PREFIX = "hold";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String HolderSocketString = BASE_URI + "HolderSocket";
    public static final Resource HolderSocket = m.createResource(HolderSocketString);
    public static final String HoldableSocketString = BASE_URI + "HoldableSocket";
    public static final Resource HoldableSocket = m.createResource(HoldableSocketString);
    public static final String holdsString = BASE_URI + "holds";
    public static final Resource holds = m.createResource(holdsString);
    public static final String heldByString = BASE_URI + "heldBy";
    public static final Resource heldBy = m.createResource(heldByString);
}
