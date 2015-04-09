package won.protocol.vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * User: ypanchenko
 * Date: 03.04.2015
 */
public class SFSIG {

  public static final String BASE_URI = "http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#";
  public static final String DEFAULT_PREFIX = "signature";

  private static Model m = ModelFactory.createDefaultModel();

  public static String getURI()
  {
    return BASE_URI;
  }

  public static final Resource SIGNATURE = m.createResource(BASE_URI + "Signature");
  public static final Resource GRAPH_SIGNING_METHOD = m.createResource(BASE_URI + "GraphSigningMethod");

  public static final Property HAS_GRAPH_SIGNING_METHOD = m.createProperty(BASE_URI, "hasGraphSigningMethod");
  public static final Property HAS_SIGNATURE_VALUE = m.createProperty(BASE_URI, "hasSignatureValue");
  public static final Property HAS_VERIFICATION_CERT = m.createProperty(BASE_URI, "hasVerificationCertificate");
  public static final Property HAS_DIGEST_METHOD = m.createProperty(BASE_URI, "hasDigestMethod");
  public static final Property HAS_GRAPH_CANONICALIZATION_METHOD = m.createProperty(BASE_URI, "hasGraphCanonicalizationMethod");
  public static final Property HAS_GRAPH_DIGEST_METHOD = m.createProperty(BASE_URI, "hasGraphDigestMethod");
  public static final Property HAS_GRAPH_SERIALIZATION_METHOD = m.createProperty(BASE_URI, "hasGraphSerializationMethod");
  public static final Property HAS_SIGNATURE_METHOD = m.createProperty(BASE_URI, "hasSignatureMethod");


}
