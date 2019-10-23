package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apache.jena.graph.TripleBoundary;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelExtract;
import org.apache.jena.rdf.model.RDFNode;
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
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionMessageContainer;
import won.protocol.model.ConnectionState;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageEvent;
import won.protocol.model.Socket;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
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
    @Autowired
    MessageEventRepository messageEventRepository;

    public Optional<Connection> getConnection(URI connectionURI) {
        return connectionRepository.findOneByConnectionURI(connectionURI);
    }

    public Connection getConnectionRequired(URI connectionURI) {
        return getConnection(connectionURI).orElseThrow(() -> new NoSuchConnectionException(connectionURI));
    }

    public Connection connectFromOwner(WonMessage wonMessage)
                    throws NoSuchConnectionException, NoSuchAtomException, IllegalMessageForAtomStateException,
                    ConnectionAlreadyExistsException, SocketCapacityException, IncompatibleSocketsException {
        wonMessage.getMessageType().requireType(WonMessageType.CONNECT);
        URI senderAtomURI = wonMessage.getSenderAtomURIRequired();
        URI senderNodeURI = wonMessage.getSenderNodeURIRequired();
        URI recipientAtomURI = wonMessage.getRecipientAtomURIRequired();
        URI senderSocketURI = wonMessage.getSenderSocketURIRequired();
        URI recipientSocketURI = wonMessage.getRecipientSocketURIRequired();
        failIfIsNotSocketOfAtom(Optional.of(senderSocketURI), Optional.of(senderAtomURI));
        failIfIsNotSocketOfAtom(Optional.of(recipientSocketURI), Optional.of(recipientAtomURI));
        Connection con = connectFromOwner(senderAtomURI, senderSocketURI, senderNodeURI, recipientAtomURI,
                        recipientSocketURI);
        return con;
    }

    public Connection connectFromOwner(URI senderAtomURI, URI senderSocketURI, URI senderNodeURI,
                    URI recipientAtomURI, URI recipientSocketURI)
                    throws NoSuchConnectionException, NoSuchAtomException, IllegalMessageForAtomStateException,
                    ConnectionAlreadyExistsException, SocketCapacityException, IncompatibleSocketsException {
        Objects.requireNonNull(senderAtomURI);
        Objects.requireNonNull(senderNodeURI);
        Objects.requireNonNull(recipientAtomURI);
        Objects.requireNonNull(senderSocketURI);
        Objects.requireNonNull(recipientSocketURI);
        Optional<Connection> con;
        // we did not know about this connection. try to find out if one exists that we
        // can use
        // the effect of connect should not be surprising. either use specified sockets
        // (if they are) or use default sockets.
        // don't try to be clever and look for suggested connections with other sockets
        // because that leads
        // consecutive connects opening connections between different sockets
        //
        // hence, we can determine our sockets now, before looking at what's there.
        Socket actualSocket = socketService.getSocket(senderAtomURI, Optional.of(senderSocketURI));
        Optional<URI> actualSocketURI = Optional.of(actualSocket.getSocketURI());
        Optional<URI> actualTargetSocketURI = Optional.of(recipientSocketURI);
        failIfIsNotSocketOfAtom(Optional.of(recipientSocketURI), Optional.of(recipientAtomURI));
        con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(
                        senderAtomURI, recipientAtomURI, actualSocketURI.get(), actualTargetSocketURI.get());
        if (!con.isPresent()) {
            // did not find such a connection either. We can safely create a new one
            // create Connection in Database
            con = Optional.of(createConnection(senderAtomURI, recipientAtomURI,
                            actualSocket.getSocketURI(), actualSocket.getTypeURI(), actualTargetSocketURI.get(),
                            Optional.empty(),
                            ConnectionState.REQUEST_SENT, ConnectionEventType.OWNER_CONNECT));
        }
        failForExceededCapacity(con.get().getSocketURI());
        failForIncompatibleSockets(con.get().getSocketURI(), con.get().getTargetSocketURI());
        // state transiation
        con.get().setState(con.get().getState().transit(ConnectionEventType.OWNER_CONNECT));
        connectionRepository.save(con.get());
        return con.get();
    }

    public Connection connectFromNode(WonMessage wonMessage) {
        // an atom wants to connect.
        // get the required data from the message and create a connection
        wonMessage.getMessageTypeRequired().requireType(WonMessageType.CONNECT);
        URI recipientAtomUri = wonMessage.getRecipientAtomURIRequired();
        URI recipientSocketURI = wonMessage.getRecipientSocketURIRequired();
        URI wonNodeUriFromWonMessage = wonMessage.getRecipientNodeURIRequired();
        URI senderAtomUri = wonMessage.getSenderAtomURIRequired();
        URI senderSocketURI = wonMessage.getSenderSocketURIRequired();
        URI senderURI = wonMessage.getSenderURIRequired();
        return connectFromNode(recipientAtomUri, recipientSocketURI, wonNodeUriFromWonMessage, senderAtomUri,
                        senderSocketURI, senderURI);
    }

    private Connection connectFromNode(URI recipientAtomUri, URI recipientSocketURI, URI wonNodeUriFromWonMessage,
                    URI senderAtomUri, URI senderSocketURI, URI senderURI) {
        Objects.requireNonNull(recipientAtomUri);
        Objects.requireNonNull(recipientSocketURI);
        Objects.requireNonNull(wonNodeUriFromWonMessage);
        Objects.requireNonNull(senderAtomUri);
        Objects.requireNonNull(senderSocketURI);
        Objects.requireNonNull(senderURI);
        failIfIsNotSocketOfAtom(Optional.of(recipientSocketURI), Optional.of(recipientAtomUri));
        Socket socket = socketService.getSocket(recipientAtomUri, Optional.of(recipientSocketURI));
        // the remote socket must be specified in a message coming from another node
        failIfIsNotSocketOfAtom(Optional.of(senderSocketURI), Optional.of(senderAtomUri));
        Connection con = null;
        // the sender did not know about our connection. try to find out if one exists
        // that we can use
        // we know which remote socket to connect to. There may be a connection with
        // that information already, either because the hint pointed to the remote
        // socket or because the connection is already in a different state and this
        // is a duplicate connect..
        Optional<Connection> conOpt = connectionRepository
                        .findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(recipientAtomUri,
                                        senderAtomUri, socket.getSocketURI(), senderSocketURI);
        if (conOpt.isPresent()) {
            con = conOpt.get();
        }
        failForExceededCapacity(socket.getSocketURI());
        failForIncompatibleSockets(socket.getSocketURI(), senderSocketURI);
        if (con == null) {
            // create Connection in Database
            con = createConnection(recipientAtomUri, senderAtomUri,
                            socket.getSocketURI(), socket.getTypeURI(), senderSocketURI, Optional.of(senderURI),
                            ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_CONNECT);
        }
        con.setTargetSocketURI(senderSocketURI);
        con.setState(con.getState().transit(ConnectionEventType.PARTNER_CONNECT));
        connectionRepository.save(con);
        return con;
    }

    /**
     * Creates a new Connection object.
     *
     * @param connectionURI
     * @param atomURI
     * @param otherAtomURI
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
    public Connection createConnection(final URI atomURI, final URI otherAtomURI,
                    final URI socketURI, final URI socketTypeURI,
                    final URI targetSocketURI, final Optional<URI> targetConnectionURI,
                    final ConnectionState connectionState,
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
        URI connectionUri = wonNodeInformationService.generateConnectionURI();
        Connection con = new Connection();
        // create and set new uri
        con.setConnectionURI(connectionUri);
        if (targetConnectionURI.isPresent()) {
            con.setTargetConnectionURI(targetConnectionURI.get());
        }
        con.setAtomURI(atomURI);
        con.setState(connectionState);
        con.setTargetAtomURI(otherAtomURI);
        con.setTypeURI(socketTypeURI);
        con.setSocketURI(socketURI);
        if (targetSocketURI != null) {
            con.setTargetSocketURI(targetSocketURI);
        }
        ConnectionMessageContainer connectionMessageContainer = new ConnectionMessageContainer(con, connectionUri);
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

    public boolean shouldSendAutoOpenForConnect(WonMessage wonMessage) {
        Optional<URI> atomURI = Optional.of(wonMessage.getRecipientAtomURI());
        Optional<URI> connectionURI = Optional.of(wonMessage.getRecipientURI());
        if (!atomURI.isPresent()) {
            return false;
        }
        return shouldSendAutoOpenForConnect(connectionURI);
    }

    public boolean shouldSendAutoOpenForConnect(Optional<URI> connectionURI) {
        if (connectionURI.isPresent()) {
            Connection con = getConnectionRequired(connectionURI.get());
            return socketService.isAutoOpen(con.getSocketURI());
        }
        return false;
    }

    public boolean shouldSendAutoOpenForOpen(WonMessage wonMessage) {
        Optional<URI> atomURI = Optional.of(wonMessage.getRecipientAtomURI());
        Optional<URI> connectionURI = Optional.of(wonMessage.getRecipientURI());
        if (!atomURI.isPresent()) {
            return false;
        }
        return shouldSendAutoOpenForOpen(connectionURI);
    }

    private boolean shouldSendAutoOpenForOpen(Optional<URI> connectionURI) {
        if (connectionURI.isPresent()) {
            Connection con = getConnectionRequired(connectionURI.get());
            if (con.getState() == ConnectionState.REQUEST_RECEIVED) {
                Socket socket = socketService.getSocketRequired(con.getSocketURI());
                Optional<URI> targetSocket = socketService.getSocketType(con.getTargetSocketURI());
                if (targetSocket.isPresent() && socketService.isAutoOpen(socket.getSocketURI())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void successResponseFromNode(WonMessage responseMessage) {
        responseMessage.getMessageType().requireType(WonMessageType.SUCCESS_RESPONSE);
        WonMessageType responseToType = responseMessage.getIsResponseToMessageType();
        URI senderURI = responseMessage.getSenderURI();
        URI isResponseToMessageURI = responseMessage.getIsResponseToMessageURI();
        URI parentURI = WonMessageUtils.getParentEntityUri(responseMessage);
        successResponseFromNode(senderURI, isResponseToMessageURI, responseToType, parentURI);
    }

    public void successResponseFromNode(URI senderURI, URI isResponseToMessageURI, WonMessageType responseToType,
                    URI parentURI) {
        // only process successResponse of connect message
        if (WonMessageType.CONNECT.equals(responseToType)) {
            MessageEvent mep = this.messageEventRepository
                            .findOneByCorrespondingRemoteMessageURIAndParentURI(isResponseToMessageURI, parentURI);
            // update the connection database: set the remote connection URI just obtained
            // from the response
            Optional<Connection> con = this.connectionRepository.findOneByConnectionURIForUpdate(mep.getSenderURI());
            con.get().setTargetConnectionURI(senderURI);
            this.connectionRepository.save(con.get());
        }
    }

    public Connection closeFromOwner(WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.CLOSE);
        URI connectionURI = wonMessage.getSenderURI();
        return closeFromOwner(connectionURI);
    }

    public Connection closeFromOwner(URI connectionURI) {
        Connection con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI).get();
        return nextConnectionState(con, ConnectionEventType.OWNER_CLOSE);
    }

    public Connection closeFromSystem(WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.CLOSE);
        URI connectionURI = wonMessage.getSenderURI();
        return closeFromSystem(connectionURI);
    }

    public Connection closeFromSystem(URI connectionURI) {
        Connection con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI).get();
        ConnectionState originalState = con.getState();
        // TODO: we could introduce SYSTEM_CLOSE here
        con = nextConnectionState(con, ConnectionEventType.OWNER_CLOSE);
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

    public Connection closeFromNode(WonMessage wonMessage) {
        URI connectionURIFromWonMessage = wonMessage.getRecipientURI();
        return closeFromNode(connectionURIFromWonMessage);
    }

    private Connection closeFromNode(URI connectionUri) {
        return nextConnectionState(connectionUri,
                        ConnectionEventType.PARTNER_CLOSE);
    }

    /**
     * Finds feedback in the message, processes it and removes it from the message.
     *
     * @param con the feedback should be processed in
     * @param message contains the feedback
     */
    public void hintFeedbackFromOwner(final WonMessage message) {
        message.getMessageType().requireType(WonMessageType.HINT_FEEDBACK_MESSAGE);
        Connection con = getConnectionRequired(message.getSenderURI());
        final URI messageURI = message.getMessageURI();
        RdfUtils.visit(message.getMessageContent(), model -> {
            Resource baseResource = model.getResource(messageURI.toString());
            if (baseResource.hasProperty(WONCON.feedback)) {
                // add the base resource as a feedback event to the connection
                processFeedback(con, baseResource);
            }
            return null;
        });
    }

    public void processFeedback(Connection connection, final RDFNode feedbackNode) {
        if (!feedbackNode.isResource()) {
            logger.warn("feedback node is not a resource, cannot process feedback for {}",
                            connection.getConnectionURI());
            return;
        }
        final Resource feedbackRes = (Resource) feedbackNode;
        if (!addFeedback(connection, feedbackRes)) {
            logger.warn("failed to add feedback to resource {}", connection.getConnectionURI());
        }
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
                throw new SocketCapacityException(
                                "Connect would exceed socket " + socketURI + " capacity of " + capacity.get());
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
