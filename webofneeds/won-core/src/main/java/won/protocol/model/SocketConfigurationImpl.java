package won.protocol.model;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

public class SocketConfigurationImpl implements SocketConfiguration {
    private URI socketType;
    private Set<URI> derivationProperties = new HashSet<>();
    private Set<URI> allowedTargetSocketTypes = new HashSet<>();
    private Optional<Boolean> autoOpen = Optional.empty();
    private Optional<Integer> capacity = Optional.empty();
    private Optional<OverloadPolicy> overloadPolicy = Optional.empty();
    private Optional<SchedulingPolicy> schedulingPolicy = Optional.empty();

    @Override
    public URI getSocketType() {
        return socketType;
    }

    @Override
    public Set<URI> getDerivationProperties() {
        return derivationProperties;
    }

    @Override
    public boolean isConnectionAllowedToType(URI targetSocketType) {
        if (allowedTargetSocketTypes.isEmpty())
            return true;
        return allowedTargetSocketTypes.contains(targetSocketType);
    }

    @Override
    public boolean isAutoOpen(URI targetSocketType) {
        return autoOpen.orElse(false);
    }

    @Override
    public Optional<Integer> getCapacity() {
        return capacity;
    }

    @Override
    public OverloadPolicy getOverloadPolicy() {
        return overloadPolicy.orElse(OverloadPolicy.ON_OVERLOAD_DENY);
    }

    @Override
    public Optional<SchedulingPolicy> getSchedulingPolicy() {
        return schedulingPolicy;
    }

    public void setSocketType(URI socketType) {
        this.socketType = socketType;
    }

    public void setDerivationProperties(Collection<URI> derivationProperties) {
        this.derivationProperties.addAll(derivationProperties);
    }

    public void addDerivationProperty(URI p) {
        this.derivationProperties.add(p);
    }

    public void setAllowedTargetSocketTypes(Collection<URI> allowedTargetSocketTypes) {
        this.allowedTargetSocketTypes.addAll(allowedTargetSocketTypes);
    }

    public void addAllowedTargetSocketType(URI target) {
        this.allowedTargetSocketTypes.add(target);
    }

    public void setAutoOpen(boolean autoOpen) {
        this.autoOpen = Optional.of(autoOpen);
    }

    public void setAutoOpen(Optional<Boolean> autoOpenOpt) {
        this.autoOpen = autoOpenOpt;
    }

    public void setCapacity(int capacity) {
        this.capacity = Optional.of(capacity);
    }

    public void setCapacity(Optional<Integer> capacity) {
        this.capacity = capacity;
    }

    public void setOverloadPolicy(OverloadPolicy overloadPolicy) {
        this.overloadPolicy = Optional.of(overloadPolicy);
    }

    public void setOverloadPolicy(Optional<OverloadPolicy> overloadPolicy) {
        this.overloadPolicy = overloadPolicy;
    }

    public void setSchedulingPolicy(SchedulingPolicy schedulingPolicy) {
        this.schedulingPolicy = Optional.of(schedulingPolicy);
    }

    public void setSchedulingPolicy(Optional<SchedulingPolicy> schedulingPolicy) {
        this.schedulingPolicy = schedulingPolicy;
    }
}
