package won.node.service.persistence;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.node.service.linkeddata.lookup.SocketLookup;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.model.Socket;
import won.protocol.model.SocketDefinition;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.SocketRepository;

/**
 * Service that is informed of a state change of a connection and performs data
 * derivation work, changing the data of the atom owning the connection.
 */
@Component
public class SocketService {
    Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    SocketRepository socketRepository;
    Map<URI, SocketDefinition> knownSockets = new HashMap<>();
    @Autowired
    SocketLookup socketLookup;

    public SocketService() {
    }

    public Optional<Socket> getDefaultSocket(URI atomUri) throws NoSuchAtomException {
        List<Socket> sockets = socketRepository.findByAtomURI(atomUri);
        for (Socket socket : sockets) {
            if (socket.isDefaultSocket())
                return Optional.of(socket);
        }
        return sockets.stream().findFirst();
    }

    public Socket getSocket(URI atomUri, Optional<URI> socketUri) throws IllegalArgumentException, NoSuchAtomException {
        if (socketUri.isPresent()) {
            return socketRepository.findByAtomURIAndSocketURI(atomUri, socketUri.get()).stream().findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                            "No socket found: atom: " + atomUri + ", socket:" + socketUri.get()));
        }
        return getDefaultSocket(atomUri)
                        .orElseThrow(() -> new IllegalArgumentException("No default socket found: atom: " + atomUri));
    }

    public Optional<URI> lookupDefaultSocket(URI atomURI) {
        return socketLookup.lookupDefaultSocket(atomURI);
    }

    public Optional<SocketDefinition> getSocketConfig(URI socketType) {
        return socketLookup.getSocketConfig(socketType);
    }

    public Optional<Integer> getCapacity(URI socket) {
        return socketLookup.getCapacity(socket);
    }

    public boolean isCompatible(URI localSocket, URI targetSocket) {
        return socketLookup.isCompatible(localSocket, targetSocket);
    }

    public boolean isAutoOpen(URI localSocket) {
        return socketLookup.isAutoOpen(localSocket);
    }
}
