package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;
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
import org.springframework.stereotype.Component;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.IncompatibleSocketsException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.SocketCapacityException;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionMessageContainer;
import won.protocol.model.ConnectionState;
import won.protocol.model.DatasetHolder;
import won.protocol.model.Socket;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONCON;

@Component
public class ConnectionService {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    ConnectionRepository connectionRepository;
    @Autowired
    SocketService socketService;
    @Autowired
    WonNodeInformationService wonNodeInformationService;
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    SocketRepository socketRepository;
    @Autowired
    ConnectionContainerRepository connectionContainerRepository;
    @Autowired
    ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    DatasetHolderRepository datasetHolderRepository;

    public Connection connectFromOwner(URI senderAtomURI, URI senderNodeURI, URI recipientAtomURI,
                    Optional<URI> userDefinedSocketURI, Optional<URI> userDefinedTargetSocketURI,
                    Optional<URI> connectionURI)
                    throws NoSuchConnectionException, NoSuchAtomException, IllegalMessageForAtomStateException,
                    ConnectionAlreadyExistsException, SocketCapacityException, IncompatibleSocketsException {
        Optional<Connection> con;
        if (connectionURI.isPresent()) {
            // we know the connection: load it
            con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI.get());
            if (!con.isPresent())
                throw new NoSuchConnectionException(connectionURI.get());
            // however, if the sockets don't match, we report an error:
            if (userDefinedSocketURI.isPresent() && !userDefinedSocketURI.get().equals(con.get().getSocketURI())) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_OWNER. Specified socket uri conflicts with existing connection data");
            }
            // remote socket uri: may be set on the connection, in which case we may have a
            // conflict
            if (con.get().getTargetSocketURI() != null && userDefinedTargetSocketURI.isPresent()
                            && !con.get().getTargetSocketURI().equals(userDefinedTargetSocketURI.get())) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_OWNER. Specified remote socket uri conflicts with existing connection data");
            }
            // if the remote socket is not yet set on the connection, we have to set it now.
            if (con.get().getTargetSocketURI() == null) {
                con.get().setTargetSocketURI(
                                userDefinedTargetSocketURI
                                                .orElse(socketService
                                                                .lookupDefaultSocket(recipientAtomURI)
                                                                .orElseThrow(() -> new IllegalStateException(
                                                                                "No default socket found for atom "
                                                                                                + recipientAtomURI))));
            }
            // sockets are set in the connection now.
        } else {
            // we did not know about this connection. try to find out if one exists that we
            // can use
            // the effect of connect should not be surprising. either use specified sockets
            // (if they are) or use default sockets.
            // don't try to be clever and look for suggested connections with other sockets
            // because that leads
            // consecutive connects opening connections between different sockets
            //
            // hence, we can determine our sockets now, before looking at what's there.
            Socket actualSocket = socketService.getSocket(senderAtomURI, userDefinedSocketURI);
            Optional<URI> actualSocketURI = Optional.of(actualSocket.getSocketURI());
            Optional<URI> actualTargetSocketURI = Optional
                            .of(userDefinedTargetSocketURI.orElse(socketService
                                            .lookupDefaultSocket(recipientAtomURI)
                                            .orElseThrow(() -> new IllegalStateException(
                                                            "No default socket found for atom "
                                                                            + recipientAtomURI))));
            con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(
                            senderAtomURI, recipientAtomURI, actualSocketURI.get(), actualTargetSocketURI.get());
            if (!con.isPresent()) {
                // did not find such a connection. It could be the connection exists, but
                // without a remote socket
                con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndNullTargetSocketForUpdate(
                                senderAtomURI, recipientAtomURI, actualSocketURI.get());
                if (con.isPresent()) {
                    // we found a connection without a remote socket uri. we use this one and we'll
                    // have to set the remote socket uri.
                    con.get().setTargetSocketURI(actualTargetSocketURI.get());
                } else {
                    // did not find such a connection either. We can safely create a new one
                    // create Connection in Database
                    URI connectionUri = wonNodeInformationService.generateConnectionURI(senderNodeURI);
                    con = Optional.of(createConnection(connectionUri, senderAtomURI, recipientAtomURI, null,
                                    actualSocket.getSocketURI(), actualSocket.getTypeURI(), actualTargetSocketURI.get(),
                                    ConnectionState.REQUEST_SENT, ConnectionEventType.OWNER_OPEN));
                }
            }
        }
        failForExceededCapacity(con.get().getSocketURI());
        failForIncompatibleSockets(con.get().getSocketURI(), con.get().getTargetSocketURI());
        // state transiation
        con.get().setState(con.get().getState().transit(ConnectionEventType.OWNER_OPEN));
        connectionRepository.save(con.get());
        return con.get();
    }

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
        if (atom.getState() != AtomState.ACTIVE)
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
    public boolean addFeedback(final Connection connection, final Resource feedback) {
        // TODO: concurrent modifications to the model for this resource result in
        // side-effects.
        // think about locking.
        logger.debug("adding feedback to resource {}", connection);
        DatasetHolder datasetHolder = connection.getDatasetHolder();
        Dataset dataset;
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
        mainRes.addProperty(WONCON.feedbackEvent, feedback);
        ModelExtract extract = new ModelExtract(new StatementTripleBoundary(TripleBoundary.stopNowhere));
        model.add(extract.extract(feedback, feedback.getModel()));
        dataset.setDefaultModel(model);
        datasetHolder.setDataset(dataset);
        datasetHolderRepository.save(datasetHolder);
        connectionRepository.save(connection);
        logger.debug("done adding feedback for resource {}", connection);
        return true;
    }

    public void updateTargetConnectionURI(Connection con, URI targetConnectionURI) {
        if (logger.isDebugEnabled()) {
            logger.debug("updating remote connection URI of con {} to {}", con, targetConnectionURI);
        }
        con.setTargetConnectionURI(targetConnectionURI);
        connectionRepository.save(con);
    }

    public void failForExceededCapacity(URI socketURI) throws SocketCapacityException {
        Optional<Integer> capacity = socketService.getCapacity(socketURI);
        if (capacity.isPresent()) {
            // lock the connection table by socketURI to avoid a race condition
            connectionRepository.countBySocketUriForUpdate(socketURI);
            if (connectionRepository.countBySocketURIAndState(socketURI, ConnectionState.CONNECTED) >= capacity.get()) {
            }
        }
    }

    public void failForIncompatibleSockets(URI socketURI, URI targetSocketURI) throws IncompatibleSocketsException {
        if (!socketService.isCompatible(socketURI, targetSocketURI)) {
            throw new IncompatibleSocketsException(socketURI, targetSocketURI);
        }
    }

    public void failIfIsNotSocketOfAtom(Optional<URI> socketURI, Optional<URI> atomURI) {
        if (socketURI.isPresent() && atomURI.isPresent()
                        && !socketURI.get().toString().startsWith(atomURI.get().toString())) {
            throw new IllegalArgumentException(
                            "User-defined socket " + socketURI.get() + " is not a socket of atom " + atomURI.get());
        }
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
}
