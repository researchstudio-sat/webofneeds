package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Good relations vocabulary.
 * <p/>
 * User: Alan Tus Date: 21.04.13. Time: 22:23
 */
public class GR {

  public static final String BASE_URI = "http://purl.org/goodrelations/v1#";
  private static Model m = ModelFactory.createDefaultModel();

  public static final Resource OFFERING = m.createResource(BASE_URI + "Offering");

  public static final Resource DELIVERY_METHOD = m.createResource(BASE_URI + "DeliveryMethod");

  public static final Resource BUSINESS_ENTITY = m.createResource(BASE_URI + "BusinessEntity");

  public static final Resource QUANTITATIVE_VALUE = m.createResource(BASE_URI + "QuantitativeValue");
  public static final Property HAS_UNIT_OF_MEASUREMENT = m.createProperty(BASE_URI, "hasUnitOfMeasurement");
  public static final Property HAS_MIN_VALUE = m.createProperty(BASE_URI, "hasMinValue");
  public static final Property HAS_MAX_VALUE = m.createProperty(BASE_URI, "hasMaxValue");
  public static final Property HAS_VALUE = m.createProperty(BASE_URI, "hasValue");

  public static final Resource DELIVERY_MODE_DIRECT_DOWNLOAD = m
      .createResource(GRDeliveryMethod.DELIVERY_MODE_DIRECT_DOWNLOAD.getURI().toString());
  public static final Resource DELIVERY_MODE_FREIGHT = m
      .createResource(GRDeliveryMethod.DELIVERY_MODE_FREIGHT.getURI().toString());
  public static final Resource DELIVERY_MODE_MAIL = m
      .createResource(GRDeliveryMethod.DELIVERY_MODE_MAIL.getURI().toString());
  public static final Resource DELIVERY_MODE_OWN_FLEET = m
      .createResource(GRDeliveryMethod.DELIVERY_MODE_OWN_FLEET.getURI().toString());
  public static final Resource DELIVERY_MODE_PICK_UP = m
      .createResource(GRDeliveryMethod.DELIVERY_MODE_PICK_UP.getURI().toString());

  /**
   * Converts the GRDeliveryMethod Enum to a Resource.
   *
   * @param state
   * @return
   */
  public static Resource toResource(GRDeliveryMethod state) {
    switch (state) {
    case DELIVERY_MODE_DIRECT_DOWNLOAD:
      return DELIVERY_MODE_DIRECT_DOWNLOAD;
    case DELIVERY_MODE_FREIGHT:
      return DELIVERY_MODE_FREIGHT;
    case DELIVERY_MODE_MAIL:
      return DELIVERY_MODE_MAIL;
    case DELIVERY_MODE_OWN_FLEET:
      return DELIVERY_MODE_OWN_FLEET;
    case DELIVERY_MODE_PICK_UP:
      return DELIVERY_MODE_PICK_UP;
    default:
      throw new IllegalStateException("No case specified for " + state.name());
    }
  }

  /**
   * returns the URI for this schema
   *
   * @return the URI for this schema
   */
  public static String getURI() {
    return BASE_URI;
  }

}
