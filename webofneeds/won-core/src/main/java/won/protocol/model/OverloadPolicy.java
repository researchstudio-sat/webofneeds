package won.protocol.model;

import java.net.URI;

import won.protocol.vocabulary.WON;

/**
 * If the socket's capacity is reached, and another connection is requested, how to react?
 * <ul>
 * <li>DENY: deny the request immediately</li>
 * <li>QUEUE: neither deny nor accept. Wait for load to drop.</li>
 * <li>REPLACE_OLDEST: accept then new connection after closing the oldest connection</li>
 * <li>REPLACE_NEWEST: accept then new connection after closing the newest connection</li>
 * <li>REPLACE_RANDOM: accept then new connection after closing a randomly chosen connection</li>
 * </ul>
 */
public enum OverloadPolicy {
    ON_OVERLOAD_DENY("OnOverloadDeny"),
    ON_OVERLOAD_QUEUE("OnOverloadQueue"),
    ON_OVERLOAD_REPLACE_OLDEST("OnOverloadReplaceOldest"),
    ON_OVERLOAD_REPLACE_NEWEST("OnOverloadReplaceNewest"),
    ON_OVERLOAD_REPLACE_RANDOM("OnOverloadReplaceRandom");
    private String name;

    private OverloadPolicy(String name) {
        this.name = name;
    }

    public URI getURI() {
        return URI.create(WON.BASE_URI + name);
    };

    /**
     * Tries to match the given URI against all enum values.
     *
     * @param uri URI to match
     * @return matched enum, null otherwise
     */
    public static OverloadPolicy fromURI(final URI uri) {
        for (OverloadPolicy policy : values())
            if (policy.getURI().equals(uri))
                return policy;
        return null;
    }
}