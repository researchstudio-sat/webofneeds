package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * User: ypanchenko
 * Date: 03.04.2015
 */
public class SFSIG {

  public static final String BASE_URI = "http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#";
  public static final String DEFAULT_PREFIX = "sig";

  public static final String PREFIX_DIGEST_METHOD = "dm-";
  public static final String PREFIX_GRAPH_DIGEST_METHOD = "gdm-";
  public static final String PREFIX_GRAPH_CANONICALIZATION_METHOD = "gcm-";
  public static final String PREFIX_GRAPH_SERIALIZATION_METHOD = "gsm-";
  public static final String PREFIX_SIGNATURE_METHOD = "sm-";

  private static Model m = ModelFactory.createDefaultModel();

  public static String getURI()
  {
    return BASE_URI;
  }

  public static final Resource SIGNATURE = m.createResource(BASE_URI + "Signature");
  public static final Resource GRAPH_SIGNING_METHOD = m.createResource(BASE_URI + "GraphSigningMethod");

  public static final Resource GRAPH_CANONICALIZATION_METHOD_Fisteus2010 = m.createResource(BASE_URI + "gcm-fisteus-2010");
  public static final Resource GRAPH_DIGEST_METHOD_Fisteus2010 = m.createResource(BASE_URI +
                                                                              PREFIX_GRAPH_DIGEST_METHOD+"-fisteus-2010");
  public static final Resource DIGEST_METHOD_SHA_256 = m.createResource(BASE_URI+PREFIX_DIGEST_METHOD+"sha-256");
  public static final Resource GRAPH_SERIALIZATION_METHOD_TRIG = m.createResource(BASE_URI+PREFIX_GRAPH_SERIALIZATION_METHOD+"trig");
  public static final Resource SIGNATURE_METHOD_ECDSA = m.createResource(BASE_URI+PREFIX_SIGNATURE_METHOD+"ecdsa");


  public static final Property HAS_GRAPH_SIGNING_METHOD = m.createProperty(BASE_URI, "hasGraphSigningMethod");
  public static final Property HAS_SIGNATURE_VALUE = m.createProperty(BASE_URI, "hasSignatureValue");
  public static final Property HAS_VERIFICATION_CERT = m.createProperty(BASE_URI, "hasVerificationCertificate");
  public static final Property HAS_DIGEST_METHOD = m.createProperty(BASE_URI, "hasDigestMethod");
  public static final Property HAS_GRAPH_CANONICALIZATION_METHOD = m.createProperty(BASE_URI, "hasGraphCanonicalizationMethod");
  public static final Property HAS_GRAPH_DIGEST_METHOD = m.createProperty(BASE_URI, "hasGraphDigestMethod");
  public static final Property HAS_GRAPH_SERIALIZATION_METHOD = m.createProperty(BASE_URI, "hasGraphSerializationMethod");
  public static final Property HAS_SIGNATURE_METHOD = m.createProperty(BASE_URI, "hasSignatureMethod");






}
