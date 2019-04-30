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
import won.protocol.model.SocketDefinition;
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
    Map<URI, SocketDefinition> knownSockets = new HashMap<>();

    public SocketService() {
    }

    public Optional<Integer> getCapacity(URI socket) {
        Optional<SocketDefinition> localConfig = getSocketConfig(socket);
        if (!localConfig.isPresent() && localConfig.get().getCapacity().isPresent()) {
            return localConfig.get().getCapacity();
        }
        return Optional.empty();
    }

    public boolean isCompatible(URI localSocket, URI targetSocket) {
        Optional<SocketDefinition> localConfig = getSocketConfig(localSocket);
        Optional<SocketDefinition> targetConfig = getSocketConfig(targetSocket);
        if (localConfig.isPresent() && targetConfig.isPresent()) {
            return localConfig.get().isCompatibleWith(targetConfig.get());
        }
        return false;
    }

    public boolean isAutoOpen(URI localSocket) {
        Optional<SocketDefinition> socketConfig = getSocketConfig(localSocket);
        if (socketConfig.isPresent()) {
            return socketConfig.get().isAutoOpen();
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
            Optional<SocketDefinition> socketConfig = getSocketConfig(con.getSocketURI());
            if (socketConfig.isPresent()) {
                Resource atomRes = derivationModel.getResource(atom.getAtomURI().toString());
                Resource targetAtomRes = derivationModel.getResource(con.getTargetAtomURI().toString());
                if (stateChange.isConnect()) {
                    logger.info("adding data for connection {}", con.getConnectionURI());
                    socketConfig.get().getDerivationProperties().stream()
                                    .map(u -> modelToManipulate.createProperty(u.toString()))
                                    .forEach(p -> modelToManipulate.add(atomRes, p, targetAtomRes));
                    socketConfig.get().getInverseDerivationProperties().stream()
                                    .map(u -> modelToManipulate.createProperty(u.toString()))
                                    .forEach(p -> modelToManipulate.add(targetAtomRes, p, atomRes));
                } else {
                    logger.info("removing data for connection {}", con.getConnectionURI());
                    socketConfig.get().getDerivationProperties().stream()
                                    .map(u -> modelToManipulate.createProperty(u.toString()))
                                    .forEach(p -> modelToManipulate.remove(atomRes, p, targetAtomRes));
                    socketConfig.get().getInverseDerivationProperties().stream()
                                    .map(u -> modelToManipulate.createProperty(u.toString()))
                                    .forEach(p -> modelToManipulate.remove(targetAtomRes, p, atomRes));
                }
            }
            atom.incrementVersion();
            atom.getDatatsetHolder().setDataset(atomDataset);
            atomRepository.save(atom);
        }
    }

    private Optional<SocketDefinition> getSocketConfig(URI socketType) {
        try {
            return WonLinkedDataUtils.getSocketDefinition(linkedDataSource, socketType);
        } catch (Exception e) {
            logger.info("Failed to load configuation for socket type " + socketType, e);
        }
        return Optional.empty();
    }
}
