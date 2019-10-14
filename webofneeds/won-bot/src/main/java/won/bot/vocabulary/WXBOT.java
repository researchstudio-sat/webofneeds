package won.bot.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class WXBOT {
    public static final String BASE_URI = "https://w3id.org/won/ext/bot#";
    public static final String DEFAULT_PREFIX = "wx-bot";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String ServiceAtomString = BASE_URI + "ServiceAtom";
    public static final Resource ServiceAtom = m.createResource(ServiceAtomString);
}
