package won.protocol.model;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SocketDefinitionImpl implements SocketDefinition {
    private final URI socketURI;
    private Optional<URI> socketDefinition = Optional.empty();
    private Set<URI> derivationProperties = new HashSet<>();
    private Set<URI> inverseDerivationProperties = new HashSet<>();
    private Set<URI> compatibleSocketTypes = new HashSet<>();
    private Optional<Boolean> autoOpen = Optional.empty();
    private Optional<Integer> capacity = Optional.empty();
    private Set<URI> inconsistentProperties = new HashSet<URI>();

    public SocketDefinitionImpl(URI socketURI) {
        this.socketURI = socketURI;
    }

    @Override
    public URI getSocketURI() {
        return socketURI;
    }

    public Optional<URI> getSocketDefinitionURI() {
        return socketDefinition;
    }

    public void setSocketDefinitionURI(URI socketDefinitionURI) {
        this.socketDefinition = Optional.ofNullable(socketDefinitionURI);
    }

    public void setSocketDefinitionURI(Optional<URI> socketDefinitionURI) {
        this.socketDefinition = socketDefinitionURI;
    }

    @Override
    public Set<URI> getDerivationProperties() {
        return derivationProperties;
    }

    @Override
    public Set<URI> getInverseDerivationProperties() {
        return inverseDerivationProperties;
    }

    @Override
    public boolean isCompatibleWith(SocketDefinition other) {
        if (this.compatibleSocketTypes.isEmpty()) {
            return true;
        }
        if (!other.getSocketDefinitionURI().isPresent()) {
            return false;
        }
        return this.compatibleSocketTypes.contains(other.getSocketDefinitionURI().get());
    }

    @Override
    public boolean isAutoOpen() {
        return autoOpen.orElse(false);
    }

    @Override
    public Optional<Integer> getCapacity() {
        return capacity;
    }

    public void setDerivationProperties(Collection<URI> derivationProperties) {
        this.derivationProperties.clear();
        this.derivationProperties.addAll(derivationProperties);
    }

    public void setInverseDerivationProperties(Collection<URI> properties) {
        this.inverseDerivationProperties.clear();
        this.inverseDerivationProperties.addAll(properties);
    }

    public void addDerivationProperty(URI p) {
        this.derivationProperties.add(p);
    }

    public void addInverseDerivationProperty(URI p) {
        this.inverseDerivationProperties.add(p);
    }

    public void setCompatibleSocketTypes(Collection<URI> allowedTargetSocketTypes) {
        this.compatibleSocketTypes.clear();
        this.compatibleSocketTypes.addAll(allowedTargetSocketTypes);
    }

    public void addCompatibleSocketType(URI target) {
        this.compatibleSocketTypes.add(target);
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

    public void addInconsistentProperty(URI property) {
        this.inconsistentProperties.add(property);
    }

    @Override
    public Set<URI> getInconsistentProperties() {
        return inconsistentProperties;
    }
}
