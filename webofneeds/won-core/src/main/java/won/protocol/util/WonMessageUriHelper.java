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

    /**
     * Appends the specified <code>id</code> to the message URI prefix
     * <code>'wm:/'</code>.
     * 
     * @param id
     * @return
     */
    public static URI createMessageURIForId(String id) {
        return URI.create(withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX) + id);
    }

    /**
     * Get the id from a <code>messageURI</code>. For example,
     * <code>wm:/abcd1234</code> yields <code>abcd1234</code>. Removes the message
     * URI prefix <code>'wm:/'</code> and strips anything after <code>'#'</code> or
     * <code>'/'</code>, if present.
     * 
     * @param messageURI
     * @return
     */
    public static String getIdFromMessageURI(URI messageURI) {
        int charsToSkip = withTrailingSlash(WONMSG.MESSAGE_URI_PREFIX).length();
        String id = messageURI.toString().substring(charsToSkip);
        // remove everything after '/'
        int pos = id.indexOf('/');
        if (pos > -1) {
            id = id.substring(0, pos);
        }
        // remove everything after '#'
        pos = id.indexOf('#');
        if (pos > -1) {
            id = id.substring(0, pos);
        }
        return id;
    }

    private static String withTrailingSlash(String localPrefix) {
        return localPrefix.endsWith("/") ? localPrefix : localPrefix + "/";
    }

    /**
     * Returns the URI reserved for a message being created or checked. The uri is
     * {@link won.protocol.vocabulary.WONMSG.MESSAGE_SELF}
     * 
     * @return
     */
    public static URI getSelfUri() {
        return URI.create(WONMSG.MESSAGE_SELF);
    }
}
