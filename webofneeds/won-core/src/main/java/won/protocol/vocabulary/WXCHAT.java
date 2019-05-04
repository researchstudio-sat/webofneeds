package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class WXCHAT {
    public static final String BASE_URI = "https://w3id.org/won/ext/chat#";
    public static final String DEFAULT_PREFIX = "chat";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String ChatSocketString = BASE_URI + "ChatSocket";
    public static final Resource ChatSocket = m.createResource(ChatSocketString);
}
