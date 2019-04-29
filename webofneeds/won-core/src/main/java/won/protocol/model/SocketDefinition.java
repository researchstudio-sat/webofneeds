package won.protocol.model;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface SocketDefinition {
    /**
     * The concrete socket of this configuration.
     */
    public URI getSocketURI();

    /**
     * If this configuration is identified by a URI, returns it, otherwise, i.e. if
     * the configuration is specified via a blank node, returns Optional.empty.
     */
    public Set<URI> getConfigurationURIs();

    /**
     * Returns the types of the socket.
     */
    public Collection<URI> getSocketTypes();

    /**
     * Returns the set of derivation properties to be used for this type.
     */
    public Set<URI> getDerivationProperties();

    /**
     * Indicates if a connection between this socket and the specified socket is
     * allowed.
     */
    public Set<URI> compatibleTypes();

    /**
     * Indicates whether this socket configuration supports connecting to the other
     * one.
     */
    public boolean isCompatibleWith(SocketDefinition other);

    /**
     * If true, the atom will automatically accept connection requests if the
     * socket's configuration does not forbid it.
     */
    public boolean isAutoOpen();

    /**
     * Indicates how many established connections are supported by the socket.
     */
    public Optional<Integer> getCapacity();

    /**
     * If the configuration is inconsistent, returns the set of properties for which
     * inconsistencies have been detected.
     **/
    public Set<URI> getInconsistentProperties();
}
