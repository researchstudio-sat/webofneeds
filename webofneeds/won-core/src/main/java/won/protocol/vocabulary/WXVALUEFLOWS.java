package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class WXVALUEFLOWS {
    public static final String BASE_URI = "https://w3id.org/won/ext/valueflows#";
    public static final String DEFAULT_PREFIX = "vf";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String PrimaryAccountableSocketString = BASE_URI + "PrimaryAccountableSocket";
    public static final Resource PrimaryAccountableSocket = m.createResource(PrimaryAccountableSocketString);
    public static final String PrimaryAccountableInverseSocketString = BASE_URI + "PrimaryAccountableIverseSocket";
    public static final Resource PrimaryAccountableIverseSocket = m.createResource(PrimaryAccountableInverseSocketString);
    public static final String CustodianSocketSocketString = BASE_URI + "CustodianSocket";
    public static final Resource CustodianSocket = m.createResource(CustodianSocketSocketString);
    public static final String CustodianInverseSocketSocketString = BASE_URI + "CustodianInverseSocket";
    public static final Resource CustodianInverseSocket = m.createResource(CustodianInverseSocketSocketString);
}
