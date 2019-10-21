package won.node.service.linkeddata.lookup;

import java.net.URI;
import java.util.Optional;

import won.protocol.model.SocketDefinition;

public interface SocketLookup {
    Optional<URI> lookupDefaultSocket(URI atomURI);

    public Optional<SocketDefinition> getSocketConfig(URI socketType);

    public Optional<Integer> getCapacity(URI socket);

    public boolean isCompatible(URI localSocket, URI targetSocket);

    public boolean isAutoOpen(URI localSocket);
}
