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
import won.protocol.exception.NoAtomForSocketFoundException;
import won.protocol.exception.NoDefaultSocketException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchSocketException;
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
            return socketRepository.findOneBySocketURI(socketUri.get())
                            .orElseThrow(() -> new NoSuchSocketException(socketUri.get()));
        }
        return getDefaultSocket(atomUri)
                        .orElseThrow(() -> new NoDefaultSocketException(atomUri));
    }

    public Optional<URI> getAtomOfSocket(URI socketURI) {
        if (socketURI == null) {
            return Optional.empty();
        }
        String uri = socketURI.toString();
        String fragment = socketURI.getRawFragment();
        if (fragment == null) {
            return Optional.empty();
        }
        return Optional.of(URI.create(uri.substring(0, uri.length() - fragment.length() - 1)));
    }

    public URI getAtomOfSocketRequired(URI socketURI) {
        return getAtomOfSocket(socketURI).orElseThrow(() -> new NoAtomForSocketFoundException(socketURI));
    }

    public Optional<Socket> getSocket(URI socketURI) {
        return socketRepository.findOneBySocketURI(socketURI);
    }

    public Socket getSocketRequired(URI socketURI) {
        return getSocket(socketURI).orElseThrow(() -> new NoSuchSocketException(socketURI));
    }

    public Optional<SocketDefinition> getSocketConfig(URI socketType) {
        return socketLookup.getSocketConfig(socketType);
    }

    public Optional<Integer> getCapacity(URI localSocket) {
        Optional<Socket> socket = socketRepository.findOneBySocketURI(localSocket);
        if (!socket.isPresent()) {
            throw new NoSuchSocketException(localSocket);
        }
        return socketLookup.getCapacityOfType(socket.get().getTypeURI());
    }

    public boolean isCompatible(URI localSocket, URI targetSocket) {
        Optional<Socket> socket = socketRepository.findOneBySocketURI(localSocket);
        if (!socket.isPresent()) {
            throw new NoSuchSocketException(localSocket);
        }
        Optional<Socket> target = socketRepository.findOneBySocketURI(targetSocket);
        Optional<URI> targetType = Optional.empty();
        if (target.isPresent()) {
            targetType = Optional.of(target.get().getTypeURI());
        } else {
            targetType = socketLookup.getSocketType(targetSocket);
        }
        if (targetType.isPresent()) {
            return socketLookup.isCompatibleSocketTypes(socket.get().getTypeURI(), targetType.get());
        }
        return false;
    }

    public boolean isAutoOpen(URI localSocket) {
        Optional<Socket> socket = socketRepository.findOneBySocketURI(localSocket);
        if (!socket.isPresent()) {
            throw new NoSuchSocketException(localSocket);
        }
        return socketLookup.isAutoOpenSocketType(socket.get().getTypeURI());
    }

    public Optional<URI> getSocketType(URI socketURI) {
        return socketLookup.getSocketType(socketURI);
    }
}
