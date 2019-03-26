package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class WONAGR {
  public static final String BASE_URI = "http://purl.org/webofneeds/agreement#";
  private static Model m = ModelFactory.createDefaultModel();
  public static Property PROPOSES = m.createProperty(BASE_URI + "proposes");
  public static Property PROPOSES_TO_CANCEL = m.createProperty(BASE_URI + "proposesToCancel");
  public static Property ACCEPTS = m.createProperty(BASE_URI + "accepts");
  public static Property REJECTS = m.createProperty(BASE_URI + "rejects");
  public static Property CLAIMS = m.createProperty(BASE_URI + "claims");

}
