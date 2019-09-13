package won.protocol.message;

import org.apache.jena.rdf.model.Resource;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim Date: 17.02.2015
 */
public enum WonMessageDirection {
    FROM_OWNER(WONMSG.FromOwner), FROM_SYSTEM(WONMSG.FromSystem), FROM_EXTERNAL(WONMSG.FromExternal);
    private Resource resource;

    WonMessageDirection(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public static WonMessageDirection getWonMessageDirection(URI uri) {
        return getWonMessageDirection(WONMSG.toResource(uri));
    }

    public boolean isIdentifiedBy(URI uri) {
        if (uri == null)
            return false;
        return getResource().getURI().equals(uri.toString());
    }

    public static WonMessageDirection getWonMessageDirection(Resource resource) {
        if (WONMSG.FromOwner.equals(resource))
            return FROM_OWNER;
        if (WONMSG.FromSystem.equals(resource))
            return FROM_SYSTEM;
        if (WONMSG.FromExternal.equals(resource))
            return FROM_EXTERNAL;
        return null;
    }
}
