package won.auth.check;

import won.auth.model.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class TargetAtomCheck {
    private URI atom;
    private URI requestedTarget;
    private Set<URI> allowedSockets;
    private Set<URI> allowedSocketTypes;
    private Set<URI> allowedConnectionStates;

    public TargetAtomCheck(URI atom, URI requestedTarget) {
        this.atom = atom;
        this.requestedTarget = requestedTarget;
    }

    public TargetAtomCheck clone() {
        TargetAtomCheck clone = new TargetAtomCheck(atom, requestedTarget);
        clone.atom = atom;
        clone.requestedTarget = requestedTarget;
        clone.allowedSockets = new HashSet<>(getAllowedSockets());
        clone.allowedSocketTypes = new HashSet<>(getAllowedSocketTypes());
        clone.allowedConnectionStates = new HashSet<>(getAllowedConnectionStates());
        return clone;
    }

    public URI getAtom() {
        return atom;
    }

    public URI getRequestedTarget() {
        return requestedTarget;
    }

    public Set<URI> getAllowedSockets() {
        if (allowedSockets == null) {
            return Collections.emptySet();
        }
        return allowedSockets;
    }

    public boolean isSocketAllowed(URI socket) {
        if (hasAllowedSockets()) {
            return getAllowedSockets().contains(socket);
        }
        return true;
    }

    public boolean isSocketTypeAllowed(URI socketType) {
        if (hasAllowedSocketTypes()) {
            return getAllowedSocketTypes().contains(socketType);
        }
        return true;
    }

    public boolean isConnectionStateAllowed(URI connectionState) {
        if (hasAllowedConnectionStates()) {
            return getAllowedConnectionStates().contains(connectionState);
        }
        return true;
    }

    public boolean isConnectionStateAllowedCS(ConnectionState connectionState) {
        if (hasAllowedConnectionStates()) {
            return getAllowedConnectionStates().contains(connectionState.getValue());
        }
        return true;
    }

    public Set<URI> getAllowedSocketTypes() {
        if (allowedSocketTypes == null) {
            return Collections.emptySet();
        }
        return allowedSocketTypes;
    }

    public Set<URI> getAllowedConnectionStates() {
        if (allowedConnectionStates == null) {
            return Collections.emptySet();
        }
        return allowedConnectionStates;
    }

    public boolean hasAllowedSockets() {
        return allowedSockets != null && allowedSockets.size() > 0;
    }

    public boolean hasAllowedSocketTypes() {
        return allowedSocketTypes != null && allowedSocketTypes.size() > 0;
    }

    public boolean hasAllowedConnectionStates() {
        return allowedConnectionStates != null && allowedConnectionStates.size() > 0;
    }

    public void setAllowedSockets(Set<URI> allowedSockets) {
        this.allowedSockets = allowedSockets;
    }

    public void setAllowedSocketTypes(Set<URI> allowedSocketTypes) {
        this.allowedSocketTypes = allowedSocketTypes;
    }

    public void setAllowedConnectionStates(Set<URI> allowedConnectionStates) {
        this.allowedConnectionStates = allowedConnectionStates;
    }

    public void setAllowedConnectionStatesCS(Set<ConnectionState> connectionStates) {
        if (connectionStates != null) {
            this.allowedConnectionStates = connectionStates.stream()
                            .map(ConnectionState::getValue)
                            .collect(Collectors.toSet());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TargetAtomCheck that = (TargetAtomCheck) o;
        return Objects.equals(atom, that.atom) &&
                        Objects.equals(requestedTarget, that.requestedTarget) &&
                        Objects.equals(allowedSockets, that.allowedSockets) &&
                        Objects.equals(allowedSocketTypes, that.allowedSocketTypes) &&
                        Objects.equals(allowedConnectionStates, that.allowedConnectionStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(atom, requestedTarget, allowedSockets, allowedSocketTypes, allowedConnectionStates);
    }

    public void intersectAllowedConnectionStates(Set<URI> allowedConnectionStates) {
        if (allowedConnectionStates != null) {
            this.allowedConnectionStates.retainAll(allowedConnectionStates);
        }
    }

    public void intersectAllowedConnectionStatesCS(Set<ConnectionState> connectionStates) {
        if (connectionStates != null) {
            intersectAllowedConnectionStates(connectionStates
                            .stream().map(ConnectionState::getValue).collect(Collectors.toSet()));
        }
    }

    @Override
    public String toString() {
        return "TargetAtomCheck{" +
                        "atom=" + atom +
                        ", requestorAtom=" + requestedTarget +
                        ", allowedSockets=" + allowedSockets +
                        ", allowedSocketTypes=" + allowedSocketTypes +
                        ", allowedConnectionStates=" + allowedConnectionStates +
                        '}';
    }
}
