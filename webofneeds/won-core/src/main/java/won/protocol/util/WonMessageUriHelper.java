package won.protocol.util;

import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import won.protocol.vocabulary.WONMSG;

public class WonMessageUriHelper {
    Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static URI toGenericMessageURI(URI localMessageURI, String localPrefix) {
        MDC.put("msgUriMappedFrom", localMessageURI != null ? localMessageURI.toString() : "null");
        if (!isLocalMessageURI(localMessageURI, localPrefix)) {
            return localMessageURI;
        }
        String prefix = withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX);
        URI mapped = URI.create(
                        prefix + localMessageURI.toString().substring(withTrailingSlash(localPrefix).length()));
        MDC.put("msgUriMappedTo", mapped != null ? mapped.toString() : "null");
        return mapped;
    }

    public static URI toLocalMessageURI(URI messageURI, String localPrefix) {
        MDC.put("msgUriMappedFrom", messageURI != null ? messageURI.toString() : "null");
        if (!isGenericMessageURI(messageURI)) {
            return messageURI;
        }
        String prefix = withTrailingSlash(localPrefix);
        URI mapped = URI.create(
                        prefix + messageURI.toString()
                                        .substring(withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX).length()));
        MDC.put("msgUriMappedTo", mapped != null ? mapped.toString() : "null");
        return mapped;
    }

    public static boolean isGenericMessageURI(URI messageURI) {
        return messageURI.toString().startsWith(withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX));
    }

    public static boolean isLocalMessageURI(URI messageURI, String localPrefix) {
        return messageURI.toString().startsWith(withTrailingSlash(localPrefix));
    }

    public static URI createMessageURIForId(String id) {
        return URI.create(withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX) + id);
    }

    private static String withTrailingSlash(String localPrefix) {
        return localPrefix.endsWith("/") ? localPrefix : localPrefix + "/";
    }
}
