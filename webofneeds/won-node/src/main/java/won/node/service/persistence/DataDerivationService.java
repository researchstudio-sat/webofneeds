package won.node.service.persistence;

import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.service.linkeddata.lookup.SocketLookup;
import won.protocol.model.*;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.vocabulary.WON;

@Component
public class DataDerivationService {
    Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired()
    SocketLookup socketLookup;
    @Autowired
    DatasetHolderRepository datasetHolderRepository;

    public DataDerivationService() {
    }

    /**
     * Performs the data derivation for the specified atom. (Currently just deletes
     * any derived data in the atom, if present)
     *
     * @param atom
     */
    public void deriveDataIfNecessary(Atom atom) {
        logger.info("Checking data derivation for atom {}", atom.getAtomURI());
        DatasetHolder datasetHolder = atom.getDatatsetHolder();
        Dataset dataset = null;
        if (datasetHolder == null) {
            dataset = DatasetFactory.createGeneral();
            datasetHolder = new DatasetHolder(atom.getAtomURI(), dataset);
            atom.setDatatsetHolder(datasetHolder);
        } else {
            dataset = datasetHolder.getDataset();
        }
        Dataset atomDataset = atom.getDatatsetHolder().getDataset();
        String derivedDataGraphUri = atom.getAtomURI() + "#derivedData";
        if (atomDataset.containsNamedModel(derivedDataGraphUri)) {
            atomDataset.removeNamedModel(derivedDataGraphUri);
            atom.getDatatsetHolder().setDataset(atomDataset);
            logger.info("Derived data for atom {}", atom.getAtomURI());
        }
        datasetHolderRepository.save(datasetHolder);
    }

    /**
     * Performs the data derivation for the specified connection.
     *
     * @param con
     */
    public void deriveDataIfNecessary(Connection con) {
        logger.debug("Checking data derivation for connection {} (state: {})", con.getConnectionURI(),
                        con.getState().getURI());
        DatasetHolder datasetHolder = con.getDatasetHolder();
        Dataset dataset = null;
        if (datasetHolder == null) {
            dataset = DatasetFactory.createGeneral();
            datasetHolder = new DatasetHolder(con.getConnectionURI(), dataset);
            con.setDatasetHolder(datasetHolder);
        } else {
            dataset = datasetHolder.getDataset();
        }
        String derivedDataGraphUri = con.getConnectionURI() + "#derivedData";
        String derivedDataMetadataGraphUri = con.getConnectionURI() + "#derivedDataMetadata";
        Model metadataModel = dataset.getNamedModel(derivedDataGraphUri);
        if (derivedDataIsUpToDate(con, metadataModel)) {
            logger.debug("Nothing to be done for connection {}", con.getConnectionURI());
            return;
        }
        dataset.addNamedModel(derivedDataGraphUri, deriveData(con));
        dataset.addNamedModel(derivedDataMetadataGraphUri, generateDerivationMetadata(con));
        con.getDatasetHolder().setDataset(dataset);
        datasetHolderRepository.save(datasetHolder);
        logger.info("Derived data for connection {}", con.getConnectionURI());
    }

    private boolean derivedDataIsUpToDate(Connection con, Model metadataModel) {
        return metadataModel.contains(
                        metadataModel.getResource(con.getConnectionURI().toString() + "#derivedData"),
                        WON.derivedForConnectionState,
                        metadataModel.getResource(con.getState().getURI().toString()));
    }

    private Model generateDerivationMetadata(Connection con) {
        Model model = ModelFactory.createDefaultModel();
        model.add(
                        model.getResource(con.getConnectionURI().toString() + "#derivedData"),
                        WON.derivedForConnectionState,
                        model.getResource(con.getState().getURI().toString()));
        return model;
    }

    private Model deriveData(Connection con) {
        Model model = ModelFactory.createDefaultModel();
        Optional<SocketDefinition> socketConfig = socketLookup.getSocketConfig(con.getSocketURI());
        if (socketConfig.isPresent()) {
            Resource atomRes = model.getResource(con.getAtomURI().toString());
            Resource targetAtomRes = model.getResource(con.getTargetAtomURI().toString());
            if (con.getState() == ConnectionState.CONNECTED) {
                logger.info("adding data for connection {}", con.getConnectionURI());
                socketConfig.get().getDerivationProperties().stream()
                                .map(u -> model.createProperty(u.toString()))
                                .forEach(p -> model.add(atomRes, p, targetAtomRes));
                socketConfig.get().getInverseDerivationProperties().stream()
                                .map(u -> model.createProperty(u.toString()))
                                .forEach(p -> model.add(targetAtomRes, p, atomRes));
            }
        }
        return model;
    }
}
