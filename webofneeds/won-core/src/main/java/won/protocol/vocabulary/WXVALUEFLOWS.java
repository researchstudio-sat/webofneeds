package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class WXVALUEFLOWS {
    public static final String BASE_URI = "https://w3id.org/won/ext/valueflows#";
    public static final String DEFAULT_PREFIX = "wx-vf";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String PrimaryAccountableSocketString = BASE_URI + "PrimaryAccountableSocket";
    public static final Resource PrimaryAccountableSocket = m.createResource(PrimaryAccountableSocketString);
    public static final String PrimaryAccountableOfSocketString = BASE_URI + "PrimaryAccountableOfSocket";
    public static final Resource PrimaryAccountableOfSocket = m
                    .createResource(PrimaryAccountableOfSocketString);
    public static final String CustodianSocketString = BASE_URI + "CustodianSocket";
    public static final Resource CustodianSocket = m.createResource(CustodianSocketString);
    public static final String CustodianOfSocketString = BASE_URI + "CustodianOfSocket";
    public static final Resource CustodianOfSocket = m.createResource(CustodianOfSocketString);
    public static final String ActorActivitySocketString = BASE_URI + "ActorActivitySocket";
    public static final Resource ActorActivitySocket = m.createResource(ActorActivitySocketString);
}
