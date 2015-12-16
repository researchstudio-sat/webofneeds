package won.protocol.vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * WGS84 Geo Positioning vocabulary.
 *
 * User: Alan Tus
 * Date: 21.04.13.
 * Time: 22:13
 */
public class CNT {

    public static final String BASE_URI = "http://www.w3.org/2011/content#";
    private static Model m = ModelFactory.createDefaultModel();

    public static final Property BYTES = m.createProperty(BASE_URI + "bytes");

    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
