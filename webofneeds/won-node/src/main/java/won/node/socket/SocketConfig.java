package won.node.socket;

import java.net.URI;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

public interface SocketConfig {
    /*
     * Returns the type that this confi is applicable for
     */
    public URI getSocketType();

    /**
     * Returns the set of derivation properties to be used for this type.
     */
    public Set<Property> getDerivationProperties();

    /**
     * Indicates if a connection between this socket and the specified socket is
     * allowed.
     */
    public boolean isConnectionAllowedToType(URI targetSocketType);

    /**
     * Indicates if a connection to the specified remote socket reacts to a connect
     * automatically with an open.
     */
    public boolean isAutoOpen(URI targetSocketType);
}
