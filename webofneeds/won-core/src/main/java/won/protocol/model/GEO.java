package won.protocol.model;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * WGS84 Geo Positioning vocabulary.
 *
 * User: Alan Tus
 * Date: 21.04.13.
 * Time: 22:13
 */
public class GEO {

    public static final String BASE_URI = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    private static Model m = ModelFactory.createDefaultModel();

    public static final Property POINT = m.createProperty(BASE_URI, "Point");
    public static final Property LONGITUDE = m.createProperty(BASE_URI, "longitude");
    public static final Property LATITUDE = m.createProperty(BASE_URI, "latitude");

    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
