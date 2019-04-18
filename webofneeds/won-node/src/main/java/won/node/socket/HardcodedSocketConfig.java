package won.node.socket;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

/**
 * Subclasses are expected to hardcode their config in their constructor.
 */
public abstract class HardcodedSocketConfig implements SocketConfig {
    private URI socketType;
    protected Set<Property> derivationProperties = new HashSet<>();

    public HardcodedSocketConfig(URI socketType) {
        this.socketType = socketType;
    }

    @Override
    public final URI getSocketType() {
        return socketType;
    }

    @Override
    final public Set<Property> getDerivationProperties() {
        return derivationProperties;
    }
}
