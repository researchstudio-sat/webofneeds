package won.node.service.linkeddata.lookup;

import java.net.URI;
import java.util.Optional;

import won.protocol.model.SocketDefinition;

public interface SocketLookup {
    Optional<SocketDefinition> getSocketConfig(URI socketType);

    Optional<Integer> getCapacity(URI socket);

    boolean isCompatible(URI localSocket, URI targetSocket);

    boolean isCompatibleSocketTypes(URI localSocketDefinition, URI targetSocketDefinition);

    boolean isAutoOpen(URI localSocket);

    boolean isAutoOpenSocketType(URI socketDefinition);

    Optional<URI> getSocketType(URI socketURI);

    Optional<SocketDefinition> getSocketConfigOfType(URI socketType);

    Optional<Integer> getCapacityOfType(URI socketType);
}
