package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class WXGROUP {
    public static final String BASE_URI = "https://w3id.org/won/ext/group#";
    public static final String DEFAULT_PREFIX = "group";
    private static Model m = ModelFactory.createDefaultModel();
    public static final String GroupSocketString = BASE_URI + "GroupSocket";
    public static final ResourceWrapper GroupSocket = ResourceWrapper.create(GroupSocketString);
    public static final String groupMemberString = BASE_URI + "groupMember";
    public static final Resource groupMember = m.createResource(groupMemberString);
}
