package won.node.service.impl;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.jena.graph.TripleBoundary;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelExtract;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StatementTripleBoundary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionMessageContainer;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.DatasetHolder;
import won.protocol.model.Socket;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WON;

/**
 * T User: gabriel Date: 06/11/13
 */
public class DataAccessServiceImpl implements won.node.service.DataAccessService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private URIService URIService;
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private SocketRepository socketRepository;
    @Autowired
    private WonNodeInformationService wonNodeInformationService;
    @Autowired
    protected ConnectionContainerRepository connectionContainerRepository;
    @Autowired
    protected AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    protected ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    protected DatasetHolderRepository datasetHolderRepository;

    /**
     * Creates a new Connection object.
     *
     * @param connectionURI
     * @param atomURI
     * @param otherAtomURI
     * @param otherConnectionURI
     * @param socketURI
     * @param socketTypeURI
     * @param targetSocketURI - optional if we don't know it yet.
     * @param connectionState
     * @param connectionEventType
     * @return
     * @throws NoSuchAtomException
     * @throws IllegalMessageForAtomStateException
     * @throws ConnectionAlreadyExistsException
     */
    public Connection createConnection(final URI connectionURI, final URI atomURI, final URI otherAtomURI,
                    final URI otherConnectionURI, final URI socketURI, final URI socketTypeURI,
                    final URI targetSocketURI, final ConnectionState connectionState,
                    final ConnectionEventType connectionEventType)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        if (atomURI == null)
            throw new IllegalArgumentException("atomURI is not set");
        if (otherAtomURI == null)
            throw new IllegalArgumentException("otherAtomURI is not set");
        if (atomURI.equals(otherAtomURI))
            throw new IllegalArgumentException("atomURI and otherAtomURI are the same");
        if (socketURI == null)
            throw new IllegalArgumentException("socketURI is not set");
        if (socketTypeURI == null)
            throw new IllegalArgumentException("socketTypeURI is not set");
        // Load atom (throws exception if not found)
        Atom atom = DataAccessUtils.loadAtom(atomRepository, atomURI);
        if (!isAtomActive(atom))
            throw new IllegalMessageForAtomStateException(atomURI, connectionEventType.name(), atom.getState());
        // TODO: create a proper exception if a socket is not supported by an atom
        if (socketRepository.findByAtomURIAndTypeURI(atomURI, socketTypeURI).isEmpty())
            throw new RuntimeException("Socket '" + socketTypeURI + "' is not supported by Atom: '" + atomURI + "'");
        /* Create connection */
        Connection con = new Connection();
        // create and set new uri
        con.setConnectionURI(connectionURI);
        con.setAtomURI(atomURI);
        con.setState(connectionState);
        con.setTargetAtomURI(otherAtomURI);
        con.setTargetConnectionURI(otherConnectionURI);
        con.setTypeURI(socketTypeURI);
        con.setSocketURI(socketURI);
        if (targetSocketURI != null) {
            con.setTargetSocketURI(targetSocketURI);
        }
        ConnectionMessageContainer connectionMessageContainer = new ConnectionMessageContainer(con, connectionURI);
        try {
            con = connectionRepository.save(con);
            connectionMessageContainerRepository.save(connectionMessageContainer);
        } catch (Exception e) {
            // we assume the unique key constraint on atomURI, targetAtomURI, typeURI was
            // violated: we have to perform an
            // update, not an insert
            logger.warn("caught exception, assuming unique key constraint on atomURI, targetAtomURI, typeURI was violated"
                            + ". Throwing a ConnectionAlreadyExistsException. TODO: think about handling this exception "
                            + "separately", e);
            throw new ConnectionAlreadyExistsException(con.getConnectionURI(), atomURI, otherAtomURI);
        }
        return con;
    }

    @Override
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

    @Override
    public Connection nextConnectionState(URI connectionURI, ConnectionEventType connectionEventType)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        if (connectionURI == null)
            throw new IllegalArgumentException("connectionURI is not set");
        // load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        // perform state transit
        ConnectionState nextState = performStateTransit(con, connectionEventType);
        // set new state and save in the db
        con.setState(nextState);
        // save in the db
        return connectionRepository.save(con);
    }

    @Override
    public Connection nextConnectionState(Connection con, ConnectionEventType connectionEventType)
                    throws IllegalMessageForConnectionStateException {
        // perform state transit
        ConnectionState nextState = performStateTransit(con, connectionEventType);
        // set new state and save in the db
        con.setState(nextState);
        // save in the db
        return connectionRepository.save(con);
    }

    /**
     * Adds feedback, represented by the subgraph reachable from feedback, to the
     * RDF description of the item identified by forResource
     * 
     * @param connection
     * @param feedback
     * @return true if feedback could be added false otherwise
     */
    @Override
    public boolean addFeedback(final Connection connection, final Resource feedback) {
        // TODO: concurrent modifications to the model for this resource result in
        // side-effects.
        // think about locking.
        logger.debug("adding feedback to resource {}", connection);
        DatasetHolder datasetHolder = connection.getDatasetHolder();
        Dataset dataset = null;
        if (datasetHolder == null) {
            // if no dataset is found, we create one.
            dataset = DatasetFactory.create();
            datasetHolder = new DatasetHolder(connection.getConnectionURI(), dataset);
            connection.setDatasetHolder(datasetHolder);
        } else {
            dataset = datasetHolder.getDataset();
        }
        Model model = dataset.getDefaultModel();
        Resource mainRes = model.getResource(connection.getConnectionURI().toString());
        if (mainRes == null) {
            logger.debug("could not add feedback to resource {}: resource not found/created in model",
                            connection.getConnectionURI());
            return false;
        }
        mainRes.addProperty(WON.feedbackEvent, feedback);
        ModelExtract extract = new ModelExtract(new StatementTripleBoundary(TripleBoundary.stopNowhere));
        model.add(extract.extract(feedback, feedback.getModel()));
        dataset.setDefaultModel(model);
        datasetHolder.setDataset(dataset);
        datasetHolderRepository.save(datasetHolder);
        connectionRepository.save(connection);
        logger.debug("done adding feedback for resource {}", connection);
        return true;
    }

    @Override
    public void updateTargetConnectionURI(Connection con, URI targetConnectionURI) {
        if (logger.isDebugEnabled()) {
            logger.debug("updating remote connection URI of con {} to {}", con, targetConnectionURI);
        }
        con.setTargetConnectionURI(targetConnectionURI);
        connectionRepository.save(con);
    }

    private boolean isAtomActive(final Atom atom) {
        return AtomState.ACTIVE == atom.getState();
    }

    /**
     * Calculates the ATConnectionState resulting from the message in the current
     * connection state. Checks if the specified message is allowed in the
     * connection's state and throws an exception if not.
     *
     * @param con
     * @param msg
     * @return
     * @throws won.protocol.exception.IllegalMessageForConnectionStateException if
     * the message is not allowed in the connection's current state
     */
    private ConnectionState performStateTransit(Connection con, ConnectionEventType msg)
                    throws IllegalMessageForConnectionStateException {
        if (!msg.isMessageAllowed(con.getState())) {
            throw new IllegalMessageForConnectionStateException(con.getConnectionURI(), msg.name(), con.getState());
        }
        return con.getState().transit(msg);
    }

    @Override
    public void setURIService(URIService URIService) {
        this.URIService = URIService;
    }
}
