package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

/**
 * Created by ksinger on 27.07.2016.
 */
public class GEOLIT {
    public static final String BASE_URI = "http://www.bigdata.com/rdf/geospatial/literals/v1#";
    private static Model m = ModelFactory.createDefaultModel();

    public static final Property LAT_LON = m.createProperty(BASE_URI, "lat-lon");

    /**
     * returns the URI for this schema
     * 
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
