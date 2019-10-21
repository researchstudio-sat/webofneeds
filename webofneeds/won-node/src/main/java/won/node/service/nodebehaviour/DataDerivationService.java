package won.node.service.nodebehaviour;

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

import won.node.service.linkeddata.lookup.SocketLookup;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.SocketDefinition;
import won.protocol.repository.AtomRepository;

@Component
public class DataDerivationService {
    Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    SocketLookup socketLookup;
    Map<URI, SocketDefinition> knownSockets = new HashMap<>();

    public DataDerivationService() {
    }

    /**
     * Performs the data derivation in response to a connection state change.
     * 
     * @param stateChange
     * @param atom
     * @param con
     * @return true if a the derived data was modified, false otherwise.
     */
    public boolean deriveDataForStateChange(ConnectionStateChange stateChange, Atom atom, Connection con) {
        if (stateChange.isConnect() || stateChange.isDisconnect()) {
            logger.info("performing data derivation for connection {}", con.getConnectionURI());
            Dataset atomDataset = atom.getDatatsetHolder().getDataset();
            Model derivationModel = atomDataset.getNamedModel(atom.getAtomURI() + "#derivedData");
            if (derivationModel == null) {
                derivationModel = ModelFactory.createDefaultModel();
                atomDataset.addNamedModel(atom.getAtomURI() + "#derivedData", derivationModel);
            }
            final Model modelToManipulate = derivationModel;
            Optional<SocketDefinition> socketConfig = socketLookup.getSocketConfig(con.getSocketURI());
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
            return true;
        }
        return false;
    }
}
