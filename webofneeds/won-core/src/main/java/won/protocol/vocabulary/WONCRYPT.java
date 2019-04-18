package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * User: ypanchenko Date: 27.03.2015
 */
public class WONCRYPT {
    public static final String BASE_URI = "https://w3id.org/won/core#";
    public static final String DEFAULT_PREFIX = "woncrypt";
    private static Model m = ModelFactory.createDefaultModel();

    public static String getURI() {
        return BASE_URI;
    }

    public static final Resource ECCPublicKey = m.createResource(BASE_URI + "ECCPublicKey");
    public static final Property ecc_curveId = m.createProperty(BASE_URI, "ecc_curveId");
    public static final Property ecc_algorithm = m.createProperty(BASE_URI, "ecc_algorithm");
    public static final Property ecc_qx = m.createProperty(BASE_URI, "ecc_qx");
    public static final Property ecc_qy = m.createProperty(BASE_URI, "ecc_qy");
    public static final String CLIENT_CERTIFICATE_HEADER = "X-Client-Certificate";
}
