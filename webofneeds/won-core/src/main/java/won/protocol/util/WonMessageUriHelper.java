package won.protocol.util;

import java.net.URI;

import won.protocol.vocabulary.WONMSG;

public class WonMessageUriHelper {
    public static URI toGenericMessageURI(URI localMessageURI, String localPrefix) {
        if (!isLocalMessageURI(localMessageURI, localPrefix)) {
            return localMessageURI;
        }
        String prefix = withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX);
        return URI.create(
                        prefix + localMessageURI.toString().substring(withTrailingSlash(localPrefix).length()));
    }

    public static URI toLocalMessageURI(URI messageURI, String localPrefix) {
        if (!isGenericMessageURI(messageURI)) {
            return messageURI;
        }
        String prefix = withTrailingSlash(localPrefix);
        return URI.create(
                        prefix + messageURI.toString()
                                        .substring(withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX).length()));
    }

    private static String withTrailingSlash(String localPrefix) {
        return localPrefix.endsWith("/") ? localPrefix : localPrefix + "/";
    }

    public static boolean isGenericMessageURI(URI messageURI) {
        return messageURI.toString().startsWith(WONMSG.MESSAGE_URI_PREFIX + "/");
    }

    public static boolean isLocalMessageURI(URI messageURI, String localPrefix) {
        return messageURI.toString().startsWith(withTrailingSlash(localPrefix));
    }

    public static URI createMessageURIForId(String id) {
        return URI.create(withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX) + id);
    }
}
