package won.protocol.model;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public interface SocketConfiguration {
    /**
     * Returns the type that this config is applicable for
     */
    public URI getSocketType();

    /**
     * Returns the set of derivation properties to be used for this type.
     */
    public Set<URI> getDerivationProperties();

    /**
     * Indicates if a connection between this socket and the specified socket is
     * allowed.
     */
    public boolean isConnectionAllowedToType(URI targetSocketType);

    /**
     * If true, the atom will automatically accept connection requests if the
     * socket's configuration does not forbid it.
     */
    public boolean isAutoOpen(URI targetSocketType);

    /**
     * Indicates how many established connections are supported by the socket.
     */
    public Optional<Integer> getCapacity();

    /**
     * Indicates how the socket handles connection requests when its capacity is
     * reached.
     */
    public OverloadPolicy getOverloadPolicy();

    /**
     * Indicates how queued requests are dequeued. Only applicable if
     * {@link OverloadPolicy.ON_OVERLOAD_QUEUE} is used.
     */
    public Optional<SchedulingPolicy> getSchedulingPolicy();
}
