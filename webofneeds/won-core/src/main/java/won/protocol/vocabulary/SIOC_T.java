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
public class SIOC_T
{

    public static final String BASE_URI = "http://rdfs.org/sioc/types#";
    private static Model m = ModelFactory.createDefaultModel();

    public static final Resource TAG = m.createResource(BASE_URI + "Tag");

    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
