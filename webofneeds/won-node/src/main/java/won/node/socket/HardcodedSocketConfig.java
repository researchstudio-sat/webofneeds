package won.node.socket;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import won.protocol.model.OverloadPolicy;
import won.protocol.model.SchedulingPolicy;
import won.protocol.model.SocketConfiguration;

/**
 * Subclasses are expected to hardcode their config in their constructor.
 */
public abstract class HardcodedSocketConfig implements SocketConfiguration {
    private URI socketType;
    protected Set<URI> derivationProperties = new HashSet<>();

    public HardcodedSocketConfig(URI socketType) {
        this.socketType = socketType;
    }

    @Override
    public final URI getSocketType() {
        return socketType;
    }

    @Override
    final public Set<URI> getDerivationProperties() {
        return derivationProperties;
    }

    @Override
    public Optional<Integer> getCapacity() {
        return Optional.empty();
    }

    @Override
    public OverloadPolicy getOverloadPolicy() {
        return OverloadPolicy.ON_OVERLOAD_DENY;
    }

    @Override
    public Optional<SchedulingPolicy> getSchedulingPolicy() {
        return Optional.empty();
    }
}
