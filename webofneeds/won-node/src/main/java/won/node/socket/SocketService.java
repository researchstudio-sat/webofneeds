package won.node.socket;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.SocketConfiguration;
import won.protocol.repository.AtomRepository;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

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
    LinkedDataSource linkedDataSource;
    Map<URI, SocketConfiguration> knownSockets = new HashMap<>();

    public SocketService() {
        addSocket(new GroupSocketConfig());
        addSocket(new ChatSocketConfig());
        addSocket(new ReviewSocketConfig());
    }

    private void addSocket(SocketConfiguration socket) {
        this.knownSockets.put(socket.getSocketType(), socket);
    }

    private Optional<SocketConfiguration> getSocket(URI socketType) {
        if (knownSockets.containsKey(socketType)) {
            return Optional.of(knownSockets.get(socketType));
        }
        attemptToLoadSocketConfig(socketType);
        if (knownSockets.containsKey(socketType)) {
            return Optional.of(knownSockets.get(socketType));
        }
        return Optional.empty();
    }

    private void attemptToLoadSocketConfig(URI socketType) {
        try {
            Optional<SocketConfiguration> config = WonLinkedDataUtils.getSocketConfiguration(linkedDataSource,
                            socketType);
            if (config.isPresent()) {
                this.knownSockets.put(socketType, config.get());
            }
        } catch (Exception e) {
            logger.info("Failed to load configuation for socket type " + socketType, e);
        }
    }

    public boolean isConnectionAllowedToType(URI localSocketType, URI targetSocketType) {
        Optional<SocketConfiguration> socketConfig = getSocket(localSocketType);
        if (socketConfig.isPresent()) {
            return socketConfig.get().isConnectionAllowedToType(targetSocketType);
        }
        return false;
    }

    public boolean isAutoOpen(URI localSocketType, URI targetSocketType) {
        Optional<SocketConfiguration> socketConfig = getSocket(localSocketType);
        if (socketConfig.isPresent()) {
            return socketConfig.get().isAutoOpen(targetSocketType);
        }
        return false;
    }

    public void deriveDataForStateChange(ConnectionStateChange stateChange, Atom atom, Connection con) {
        if (stateChange.isConnect() || stateChange.isDisconnect()) {
            logger.info("performing data derivation for connection {}", con.getConnectionURI());
            Dataset atomDataset = atom.getDatatsetHolder().getDataset();
            Model derivationModel = atomDataset.getNamedModel(atom.getAtomURI() + "#derivedData");
            if (derivationModel == null) {
                derivationModel = ModelFactory.createDefaultModel();
                atomDataset.addNamedModel(atom.getAtomURI() + "#derivedData", derivationModel);
            }
            final Model modelToManipulate = derivationModel;
            URI socketType = con.getTypeURI();
            Optional<SocketConfiguration> socketConfig = getSocket(socketType);
            if (socketConfig.isPresent()) {
                Resource atomRes = derivationModel.getResource(atom.getAtomURI().toString());
                Resource targetAtomRes = derivationModel.getResource(con.getTargetAtomURI().toString());
                if (stateChange.isConnect()) {
                    logger.info("adding data for connection {}", con.getConnectionURI());
                    socketConfig.get().getDerivationProperties().stream()
                                    .map(u -> modelToManipulate.createProperty(u.toString()))
                                    .forEach(p -> modelToManipulate.add(atomRes, p, targetAtomRes));
                } else {
                    logger.info("removing data for connection {}", con.getConnectionURI());
                    socketConfig.get().getDerivationProperties().stream()
                                    .map(u -> modelToManipulate.createProperty(u.toString()))
                                    .forEach(p -> modelToManipulate.remove(atomRes, p, targetAtomRes));
                }
            }
            atom.incrementVersion();
            atom.getDatatsetHolder().setDataset(atomDataset);
            atomRepository.save(atom);
        }
    }
}
