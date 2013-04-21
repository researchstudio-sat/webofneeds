package won.protocol.model;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * Good relations vocabulary.
 *
 * User: Alan Tus
 * Date: 21.04.13.
 * Time: 22:23
 */
public class GR {

    public static final String BASE_URI = "www.w3.org/2003/01/geo/wgs84_pos#";
    private static Model m = ModelFactory.createDefaultModel();

    public static final Property OFFERING = m.createProperty(BASE_URI, "Offering");

    public static final Property DELIVERY_METHOD = m.createProperty(BASE_URI, "DeliveryMethod");

    public static final Property x = m.createProperty(BASE_URI, "BusinessEntity");

    public static final Property QUANTITATIVE_VALUE = m.createProperty(BASE_URI, "QuantitativeValue");
    public static final Property HAS_UNIT_of_measurement = m.createProperty(BASE_URI, "hasUnitOfMeasurement");
    public static final Property HAS_MIN_VALUE = m.createProperty(BASE_URI, "hasMinValue");
    public static final Property HAS_MAX_VALUE = m.createProperty(BASE_URI, "hasMaxValue");
    public static final Property HAS_VALUE = m.createProperty(BASE_URI, "hasValue");

    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }

}
