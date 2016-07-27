package won.protocol.vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Created by ksinger on 27.07.2016.
 */
public class GEOLIT {
    public static final String BASE_URI = "http://www.bigdata.com/rdf/geospatial/literals/v1#";
    private static Model m = ModelFactory.createDefaultModel();

    public static final Property LAT_LON = m.createProperty(BASE_URI, "lat-lon");


    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
