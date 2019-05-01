package won.protocol.model;

import java.net.URI;

import won.protocol.vocabulary.WON;

/**
 * If a there is a queue of waiting connections and the atom can accept one, how
 * to choose?
 * <ul>
 * <li>FIFO_SCHEDULING: accept the waiting connection that requested first</li>
 * <li>LIFO_SCHEDULING: accept the waiting connection that requested last</li>
 * <li>RANDOM_SCHEDULING: accept a waiting connection at random</li>
 * </ul>
 */
public enum SchedulingPolicy {
    FIFO_SCHEDULING("FIFOScheduling"), LIFO_SCHEDULING("LIFOScheduling"), RANDOM_SCHEDULING("RandomScheduling");
    private String name;

    private SchedulingPolicy(String name) {
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
    public static SchedulingPolicy fromURI(final URI uri) {
        for (SchedulingPolicy policy : values())
            if (policy.getURI().equals(uri))
                return policy;
        return null;
    }
};