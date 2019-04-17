package won.node.socket;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.protocol.model.Connection;
import won.protocol.model.Atom;
import won.protocol.repository.AtomRepository;

/**
 * Service that is informed of a state change of a connection and performs data
 * derivation work, changing the data of the atom owning the connection.
 */
@Component
public class SocketService {
    Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    AtomRepository atomRepository;
    Map<URI, SocketConfig> hardcodedConfigs = new HashMap<>();

    public SocketService() {
        addConfig(new HoldableSocketConfig());
        addConfig(new HolderSocketConfig());
        addConfig(new GroupSocketConfig());
        addConfig(new ChatSocketConfig());
        addConfig(new ReviewSocketConfig());
    }

    private void addConfig(SocketConfig config) {
        this.hardcodedConfigs.put(config.getSocketType(), config);
    }

    public boolean isConnectionAllowedToType(URI localSocketType, URI targetSocketType) {
        if (hardcodedConfigs.containsKey(localSocketType)) {
            return hardcodedConfigs.get(localSocketType).isConnectionAllowedToType(targetSocketType);
        }
        return false;
    }

    public boolean isAutoOpen(URI localSocketType, URI targetSocketType) {
        if (hardcodedConfigs.containsKey(localSocketType)) {
            return hardcodedConfigs.get(localSocketType).isAutoOpen(targetSocketType);
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
            if (hardcodedConfigs.containsKey(socketType)) {
                Resource atomRes = derivationModel.getResource(atom.getAtomURI().toString());
                Resource targetAtomRes = derivationModel.getResource(con.getTargetAtomURI().toString());
                SocketConfig config = hardcodedConfigs.get(socketType);
                if (stateChange.isConnect()) {
                    logger.info("adding data for connection {}", con.getConnectionURI());
                    config.getDerivationProperties().stream()
                                    .forEach(p -> modelToManipulate.add(atomRes, p, targetAtomRes));
                } else {
                    logger.info("removing data for connection {}", con.getConnectionURI());
                    config.getDerivationProperties().stream()
                                    .forEach(p -> modelToManipulate.remove(atomRes, p, targetAtomRes));
                }
            }
            atom.incrementVersion();
            atom.getDatatsetHolder().setDataset(atomDataset);
            atomRepository.save(atom);
        }
    }
}
