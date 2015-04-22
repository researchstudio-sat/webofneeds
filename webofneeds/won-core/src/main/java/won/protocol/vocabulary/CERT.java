package won.protocol.vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * User: ypanchenko
 * Date: 27.03.2015
 */
public class CERT
{

  public static final String BASE_URI = "http://www.w3.org/ns/auth/cert#";
  public static final String DEFAULT_PREFIX= "cert";

  private static Model m = ModelFactory.createDefaultModel();


  public static final Property KEY = m.createProperty(BASE_URI + "key");
  public static final Property PUBLIC_KEY = m.createProperty(BASE_URI + "PublicKey");
  public static final Property RSA_PUBLIC_KEY = m.createProperty(BASE_URI + "RSAPublicKey");


  /** returns the URI for this schema
   * @return the URI for this schema
   */
  public static String getURI() {
    return BASE_URI;
  }
}
