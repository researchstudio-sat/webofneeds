package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class WONMATCH {
    public static final String BASE_URI = "https://w3id.org/won/matching#";
    public static final String DEFAULT_PREFIX = "match";
    private static final Model m = ModelFactory.createDefaultModel();
    public static final Property seeks = m.createProperty(BASE_URI + "seeks");
    // adds a flag to an atom
    public static final Property flag = m.createProperty(BASE_URI + "flag");
    public static final Property doNotMatchBefore = m.createProperty(BASE_URI + "doNotMatchBefore");
    public static final Property doNotMatchAfter = m.createProperty(BASE_URI + "doNotMatchAfter");
    // the usedForTesting flag: atom is not a real atom, only match with other atoms
    // flagged with usedForTesting
    public static final Resource UsedForTesting = m.createResource(BASE_URI + "UsedForTesting");
    public static final Resource WhatsAround = m.createResource(BASE_URI + "WhatsAround");
    public static final Resource WhatsNew = m.createResource(BASE_URI + "WhatsNew");
    // hint behaviour
    public static final Resource NoHintForCounterpart = m.createResource(BASE_URI + "NoHintForCounterpart");
    public static final Resource NoHintForMe = m.createResource(BASE_URI + "NoHintForMe");
    public static final Property matchingContext = m.createProperty(BASE_URI + "matchingContext");
    public static final Property sparqlQuery = m.createProperty(BASE_URI + "sparqlQuery");
    public static final Property searchString = m.createProperty(BASE_URI + "searchString");

    /**
     * Returns the base URI for this schema.
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
