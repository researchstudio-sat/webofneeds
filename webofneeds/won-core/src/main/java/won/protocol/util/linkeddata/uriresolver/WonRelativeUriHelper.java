package won.protocol.util.linkeddata.uriresolver;

import won.protocol.message.WonMessage;
import won.protocol.model.Atom;

import java.net.URI;
import java.util.Objects;

public class WonRelativeUriHelper {
    public static URI createConnectionContainerURIForAtom(URI atomURI) {
        return URI.create(atomURI.toString() + "/c");
    }

    public static URI createMessageContainerURIForAtom(URI atomURI) {
        return URI.create(atomURI.toString() + "/msg");
    }

    public static URI createMessageContainerURIForConnection(URI connURI) {
        return URI.create(connURI.toString() + "/msg");
    }

    public static URI createAclGraphURIForAtomURI(URI atomUri) {
        return URI.create(atomUri + Atom.ACL_GRAPH_URI_FRAGMENT);
    }

    public static URI createSocketAclGraphURIForAtomURI(URI atomUri) {
        return URI.create(atomUri + Atom.SOCKET_ACL_GRAPH_URI_FRAGMENT);
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

    public static URI createGrantsRequestURIForAtomURI(URI atomURI) {
        return URI.create(atomURI.toString() + "/grants");
    }

    public static URI stripFragment(URI uriWithFragment) {
        URI uriWithoutFragment;
        Objects.requireNonNull(uriWithFragment);
        // just strip the fragment
        String fragment = uriWithFragment.getRawFragment();
        if (fragment == null) {
            return uriWithFragment;
        }
        String uri = uriWithFragment.toString();
        uriWithoutFragment = URI.create(uri.substring(0, uri.length() - fragment.length() - 1));
        return uriWithoutFragment;
    }

    /**
     * Strip the connection suffix, return the atom URI.
     *
     * @param connectionURI
     * @return
     */
    public static URI stripConnectionSuffix(URI connectionURI) {
        Objects.requireNonNull(connectionURI);
        String uri = connectionURI.toString();
        return URI.create(uri.replaceFirst("/c/.+$", ""));
    }

    /**
     * Strip the atom suffix, return the WoN node URI.
     *
     * @param atomURI
     * @return
     */
    public static URI stripAtomSuffix(URI atomURI) {
        Objects.requireNonNull(atomURI);
        String uri = atomURI.toString();
        return URI.create(uri.replaceFirst("/atom/.+$", ""));
    }
}
