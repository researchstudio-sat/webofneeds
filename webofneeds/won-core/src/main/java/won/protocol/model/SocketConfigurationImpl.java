package won.protocol.model;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SocketConfigurationImpl implements SocketConfiguration {
    private final URI socketURI;
    private Set<URI> socketTypes = new HashSet<>();
    private Set<URI> configurationUris = new HashSet<>();
    private Set<URI> derivationProperties = new HashSet<>();
    private Set<URI> compatibleSocketTypes = new HashSet<>();
    private Optional<Boolean> autoOpen = Optional.empty();
    private Optional<Integer> capacity = Optional.empty();
    private Set<URI> inconsistentProperties = new HashSet<URI>();

    public SocketConfigurationImpl(URI socketURI) {
        this.socketURI = socketURI;
    }

    @Override
    public URI getSocketURI() {
        return socketURI;
    }

    @Override
    public Collection<URI> getSocketTypes() {
        return socketTypes;
    }

    @Override
    public Set<URI> getDerivationProperties() {
        return derivationProperties;
    }

    @Override
    public boolean isCompatibleWith(SocketConfiguration other) {
        return compatibleSocketTypes.stream().anyMatch(compatible -> other.getSocketTypes().contains(compatible))
                        && other.compatibleTypes().stream()
                                        .anyMatch(compatible -> this.getSocketTypes().contains(compatible));
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

    public void addDerivationProperty(URI p) {
        this.derivationProperties.add(p);
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

    public void setSocketTypes(Set<URI> socketTypes) {
        this.socketTypes = socketTypes;
    }

    public void addInconsistentProperty(URI property) {
        this.inconsistentProperties.add(property);
    }

    public void setConfigurationURIs(Collection<URI> configurationUris) {
        this.configurationUris.clear();
        this.configurationUris.addAll(configurationUris);
    }

    @Override
    public Set<URI> getConfigurationURIs() {
        return configurationUris;
    }

    @Override
    public Set<URI> compatibleTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<URI> getInconsistentProperties() {
        // TODO Auto-generated method stub
        return null;
    }
}
