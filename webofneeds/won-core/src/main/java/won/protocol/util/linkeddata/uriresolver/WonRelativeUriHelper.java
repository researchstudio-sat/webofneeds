package won.protocol.util.linkeddata.uriresolver;

import won.protocol.message.WonMessage;
import won.protocol.model.Atom;

import java.net.URI;

public class WonRelativeUriHelper {
    public static URI createConnectionContainerURIForAtom(URI atomURI) {
        return URI.create(atomURI.toString() + "/c");
    }

    public static URI createMessageContainerURIForConnection(URI connURI) {
        return URI.create(connURI.toString() + "/msg");
    }

    public static URI createAclGraphURIForAtomURI(URI atomUri) {
        return URI.create(atomUri + Atom.ACL_GRAPH_URI_FRAGMENT);
    }

    public static URI createSysInfoGraphURIForAtomURI(final URI atomURI) {
        // TODO: [SECURITY] it's possible to submit atom data that clashes with this
        // name,
        // which may lead to undefined behavior
        return URI.create(atomURI + "#sysinfo");
    }

    public static URI createKeyGraphURIForMessageURI(final URI messageURI) {
        return URI.create(messageURI.toString() + WonMessage.KEY_URI_SUFFIX);
    }

    public static URI createKeyGraphURIForAtomURI(final URI atomURI) {
        return URI.create(atomURI.toString() + WonMessage.KEY_URI_SUFFIX);
    }

    /**
     * Creates an uri for requesting won acl tokens with the provided query, which
     * is appended to the token endpoint URI as-is (no url-encoding happening here).
     *
     * @param atomURI
     * @param query may be null
     * @return
     */
    public static URI createTokenRequestURIForAtomURIWithQuery(URI atomURI, String query) {
        return URI.create(atomURI.toString() + "/token" + (query == null ? "" : "?" + query));
    }
}
