package won.protocol.vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Created by ksinger on 27.07.2016.
 */
public class GEOB {
    public static final String BASE_URI = "http://www.bigdata.com/rdf/geospatial#";
    private static Model m = ModelFactory.createDefaultModel();

    //public static final Resource POINT = m.createResource(BASE_URI + "Point");
    public static final Property LONGITUDE = m.createProperty(BASE_URI, "lonValue");
    public static final Property LATITUDE = m.createProperty(BASE_URI, "latValue");
    public static final Property LOCATION_VALUE = m.createProperty(BASE_URI, "locationValue");
    //e.g. "47.55793#7.58899"
    public static final Property SPATIAL_RECTANGLE_SOUTH_WEST = m.createProperty(BASE_URI, "spatialRectangleSouthWest");
    public static final Property SPATIAL_RECTANGLE_NORTH_EAST = m.createProperty(BASE_URI, "spatialRectangleNorthEast");

    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
