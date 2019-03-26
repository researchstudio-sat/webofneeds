package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class WONMOD {
  public static final String BASE_URI = "http://purl.org/webofneeds/modification#";

  private static Model m = ModelFactory.createDefaultModel();

  public static Property RETRACTS = m.createProperty(BASE_URI + "retracts");

}
