package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;

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
import won.protocol.exception.MissingMessagePropertyException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.SocketCapacityException;
import won.protocol.exception.WrongAddressingInformationException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
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
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WONCON;
import won.protocol.vocabulary.WONMSG;

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
    @Autowired
    EntityManager entityManager;

    public Optional<Connection> getConnection(URI connectionURI) {
        return connectionRepository.findOneByConnectionURI(connectionURI);
    }

    public Optional<Connection> getConnection(URI socketURI, URI targetsSocketURI) {
        return connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, targetsSocketURI);
    }

    public Connection getConnectionRequired(URI socketURI, URI targetSocketURI) {
        return connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, targetSocketURI)
                        .orElseThrow(() -> new NoSuchConnectionException(socketURI, targetSocketURI));
    }

    public Connection getConnectionRequired(URI connectionURI) {
        return getConnection(connectionURI).orElseThrow(() -> new NoSuchConnectionException(connectionURI));
    }

    public boolean existOpenConnections(URI atom, URI socket) {
        return connectionRepository.existsWithtomURIAndSocketURIAndState(atom, socket, ConnectionState.CONNECTED);
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
        logger.debug("connect from owner: processing message {}", wonMessage.getMessageURI());
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
        // // lock the atom so we don't make writes that cancel each other out in case
        // we
        // // process an connect from node in parallel
        // if (logger.isDebugEnabled()) {
        // logger.debug("connect from owner: locking atom {}", senderAtomURI);
        // }
        // Atom atom = atomRepository.findOneByAtomURIForUpdate(senderAtomURI);
        // entityManager.refresh(atom);
        Socket actualSocket = socketService.getSocket(senderAtomURI, Optional.of(senderSocketURI));
        Optional<URI> actualSocketURI = Optional.of(actualSocket.getSocketURI());
        Optional<URI> actualTargetSocketURI = Optional.of(recipientSocketURI);
        failIfIsNotSocketOfAtom(Optional.of(recipientSocketURI), Optional.of(recipientAtomURI));
        if (logger.isDebugEnabled()) {
            logger.debug("connect from owner: loading connection {} - {}", actualSocketURI.get(),
                            actualTargetSocketURI.get());
        }
        con = connectionRepository.findOneBySocketURIAndTargetSocketURIForUpdate(actualSocketURI.get(),
                        actualTargetSocketURI.get());
        if (!con.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("connect from owner: did not find an existing connection");
            }
            // did not find such a connection either. We can safely create a new one
            // create Connection in Database
            con = Optional.of(createConnection(senderAtomURI, recipientAtomURI,
                            actualSocket.getSocketURI(), actualSocket.getTypeURI(), actualTargetSocketURI.get(),
                            Optional.empty(),
                            ConnectionState.REQUEST_SENT, ConnectionEventType.OWNER_CONNECT));
            if (logger.isDebugEnabled()) {
                logger.debug("connect from owner: created new connection {}", con.get().getConnectionURI());
            }
        } else {
            entityManager.refresh(con.get());
            if (logger.isDebugEnabled()) {
                logger.debug("connect from owner: found existing connection {} in state {}",
                                con.get().getConnectionURI(),
                                con.get().getState());
            }
        }
        failForExceededCapacity(con.get().getSocketURI());
        failForIncompatibleSockets(con.get().getSocketURI(), con.get().getTargetSocketURI());
        // state transiation
        ConnectionState nextState = con.get().getState().transit(ConnectionEventType.OWNER_CONNECT);
        con.get().setPreviousState(con.get().getState());
        con.get().setState(nextState);
        if (logger.isDebugEnabled()) {
            logger.debug("connect from owner: set connection {} state to: {}", con.get().getConnectionURI(),
                            con.get().getState());
        }
        return connectionRepository.save(con.get());
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
        logger.debug("connect from node: processing message {}", wonMessage.getMessageURI());
        return connectFromNode(recipientAtomUri, recipientSocketURI, wonNodeUriFromWonMessage, senderAtomUri,
                        senderSocketURI);
    }

    private Connection connectFromNode(URI recipientAtomUri, URI recipientSocketURI, URI wonNodeUriFromWonMessage,
                    URI senderAtomUri, URI senderSocketURI) {
        Objects.requireNonNull(recipientAtomUri);
        Objects.requireNonNull(recipientSocketURI);
        Objects.requireNonNull(wonNodeUriFromWonMessage);
        Objects.requireNonNull(senderAtomUri);
        Objects.requireNonNull(senderSocketURI);
        failIfIsNotSocketOfAtom(Optional.of(recipientSocketURI), Optional.of(recipientAtomUri));
        Socket socket = socketService.getSocket(recipientAtomUri, Optional.of(recipientSocketURI));
        // the remote socket must be specified in a message coming from another node
        failIfIsNotSocketOfAtom(Optional.of(senderSocketURI), Optional.of(senderAtomUri));
        Connection con = null;
        // // lock the atom so we don't make writes that cancel each other out in case
        // we
        // // process an connect from owner in parallel
        // if (logger.isDebugEnabled()) {
        // logger.debug("connect from node: locking atom {}", recipientAtomUri);
        // }
        // Atom atom = atomRepository.findOneByAtomURIForUpdate(recipientAtomUri);
        // entityManager.refresh(atom);
        // the sender did not know about our connection. try to find out if one exists
        // that we can use
        // we know which remote socket to connect to. There may be a connection with
        // that information already, either because the hint pointed to the remote
        // socket or because the connection is already in a different state and this
        // is a duplicate connect..
        if (logger.isDebugEnabled()) {
            logger.debug("connect from node: loading connection {} - {}", socket.getSocketURI(), senderSocketURI);
        }
        Optional<Connection> conOpt = connectionRepository
                        .findOneBySocketURIAndTargetSocketURIForUpdate(socket.getSocketURI(), senderSocketURI);
        if (conOpt.isPresent()) {
            entityManager.refresh(conOpt.get());
            if (logger.isDebugEnabled()) {
                logger.debug("connect from node: found existing connection {} in state {}",
                                conOpt.get().getConnectionURI(),
                                conOpt.get().getState());
            }
            con = conOpt.get();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("connect from node: did not find an existing connection");
            }
        }
        failForExceededCapacity(socket.getSocketURI());
        failForIncompatibleSockets(socket.getSocketURI(), senderSocketURI);
        if (con == null) {
            // create Connection in Database
            con = createConnection(recipientAtomUri, senderAtomUri,
                            socket.getSocketURI(), socket.getTypeURI(), senderSocketURI, Optional.empty(),
                            ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_CONNECT);
            if (logger.isDebugEnabled()) {
                logger.debug("connect from node: created new connection {}", con.getConnectionURI());
            }
        }
        ConnectionState nextState = con.getState().transit(ConnectionEventType.PARTNER_CONNECT);
        con.setPreviousState(con.getState());
        con.setState(nextState);
        if (logger.isDebugEnabled()) {
            logger.debug("connect from node: set connection {} state to: {}", con.getConnectionURI(), con.getState());
        }
        return connectionRepository.save(con);
    }

    public Connection socketHint(WonMessage wonMessage) {
        URI recipientAtomURI = wonMessage.getRecipientAtomURIRequired();
        URI recipientWoNNodeURI = wonMessage.getRecipientNodeURIRequired();
        URI targetSocketURI = wonMessage.getHintTargetSocketURIRequired();
        URI recipientSocketURI = wonMessage.getRecipientSocketURIRequired();
        Double score = wonMessage.getHintScore();
        if (targetSocketURI == null) {
            throw new MissingMessagePropertyException(WONMSG.hintTargetSocket);
        }
        if (wonMessage.getHintTargetAtomURI() != null) {
            throw new WrongAddressingInformationException(
                            "A SocketHintMessage must not have a msg:hintTargetAtom property",
                            wonMessage.getMessageURI(), WONMSG.hintTargetAtom);
        }
        if (score < 0 || score > 1) {
            throw new WrongAddressingInformationException("score is not in [0,1]", wonMessage.getMessageURI(),
                            WONMSG.hintScore);
        }
        if (recipientSocketURI == null) {
            throw new MissingMessagePropertyException(WONMSG.recipientSocket);
        }
        if (!socketService.isCompatible(recipientSocketURI, targetSocketURI)) {
            throw new IncompatibleSocketsException(recipientSocketURI, targetSocketURI);
        }
        // if (logger.isDebugEnabled()) {
        // logger.debug("socket hint: locking atom {}", recipientAtomURI);
        // }
        // Atom atom = atomRepository.findOneByAtomURIForUpdate(recipientAtomURI);
        // entityManager.refresh(atom);
        Socket socket = socketService.getSocket(recipientAtomURI, Optional.ofNullable(recipientSocketURI));
        // create Connection in Database
        URI targetAtomURI = socketService.getAtomOfSocketRequired(targetSocketURI);
        Optional<Connection> con = connectionRepository
                        .findOneBySocketURIAndTargetSocketURIForUpdate(socket.getSocketURI(), targetSocketURI);
        if (con.isPresent()) {
            entityManager.refresh(con);
            if (logger.isDebugEnabled()) {
                logger.debug("socket hint: connection {} - {} already exists", con.get().getSocketURI(),
                                con.get().getTargetSocketURI());
            }
            return con.get();
        }
        Connection newCon = createConnection(recipientAtomURI, targetAtomURI,
                        recipientSocketURI, socket.getTypeURI(), targetSocketURI, Optional.empty(),
                        ConnectionState.SUGGESTED,
                        ConnectionEventType.MATCHER_HINT);
        if (logger.isDebugEnabled()) {
            logger.debug("socket hint: created connection {}", newCon.getConnectionURI());
        }
        return newCon;
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
        Optional<Atom> atom = atomRepository.findOneByAtomURIForUpdate(atomURI);
        if (atom.isPresent()) {
            entityManager.refresh(atom.get());
        }
        if (atom.get().getState() != AtomState.ACTIVE)
            throw new IllegalMessageForAtomStateException(atomURI, connectionEventType.name(), atom.get().getState());
        // TODO: create a proper exception if a socket is not supported by an atom
        if (socketRepository.findByAtomURIAndTypeURI(atomURI, socketTypeURI).isEmpty())
            throw new RuntimeException("Socket '" + socketTypeURI + "' is not supported by Atom: '" + atomURI + "'");
        /* Create connection */
        URI connectionUri = wonNodeInformationService.generateConnectionURI(atomURI);
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
        URI recipientSocketURI = wonMessage.getRecipientSocketURIRequired();
        return socketService.isAutoOpen(recipientSocketURI);
    }

    public void grabRemoteConnectionURIFromRemoteResponse(WonMessage responseMessage) {
        responseMessage.getMessageType().requireType(WonMessageType.SUCCESS_RESPONSE);
        WonMessageType responseToType = responseMessage.getRespondingToMessageType();
        URI senderURI = responseMessage.getConnectionURIRequired(); // the remote connection uri
        URI senderSocketURI = responseMessage.getSenderSocketURIRequired();
        URI recipientSocketURI = responseMessage.getRecipientSocketURIRequired();
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(recipientSocketURI,
                        senderSocketURI);
        if (con.isPresent() && senderURI != null) {
            con.get().setTargetConnectionURI(senderURI);
            if (logger.isDebugEnabled()) {
                logger.debug("Grabbed targetURI {} for connectionURI {} from success response {}", new Object[] {
                                senderURI, con.get().getConnectionURI(), responseMessage.getMessageURI() });
            }
            connectionRepository.save(con.get());
        }
    }

    public Connection closeFromOwner(WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.CLOSE);
        return close(wonMessage.getSenderSocketURIRequired(),
                        wonMessage.getRecipientSocketURIRequired());
    }

    public Connection closeFromSystem(WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.CLOSE);
        return close(wonMessage.getSenderSocketURIRequired(), wonMessage.getRecipientSocketURIRequired());
    }

    private Connection close(URI socketURI, URI targetSocketURI) {
        Connection con = getConnectionRequired(socketURI, targetSocketURI);
        return nextConnectionState(con, ConnectionEventType.OWNER_CLOSE);
    }

    private Connection nextConnectionState(URI connectionURI, ConnectionEventType connectionEventType)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        if (connectionURI == null)
            throw new IllegalArgumentException("connectionURI is not set");
        // load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        // perform state transit
        ConnectionState nextState = performStateTransit(con, connectionEventType);
        // set new state and save in the db
        con.setPreviousState(con.getState());
        con.setState(nextState);
        // save in the db
        return connectionRepository.save(con);
    }

    public Connection nextConnectionState(Connection con, ConnectionEventType connectionEventType)
                    throws IllegalMessageForConnectionStateException {
        // perform state transit
        ConnectionState nextState = performStateTransit(con, connectionEventType);
        // set new state and save in the db
        con.setPreviousState(con.getState());
        con.setState(nextState);
        // save in the db
        return connectionRepository.save(con);
    }

    public Connection closeFromNode(WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.CLOSE);
        return close(wonMessage.getRecipientSocketURIRequired(), wonMessage.getSenderSocketURIRequired());
    }

    /**
     * Finds feedback in the message, processes it and removes it from the message.
     *
     * @param con the feedback should be processed in
     * @param message contains the feedback
     */
    public void hintFeedbackFromOwner(final WonMessage message) {
        message.getMessageType().requireType(WonMessageType.HINT_FEEDBACK_MESSAGE);
        Connection con = getConnectionRequired(message.getConnectionURIRequired());
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

    public Connection getConnectionForMessageRequired(WonMessage message, WonMessageDirection direction) {
        return getConnectionForMessage(message, direction)
                        .orElseThrow(() -> new NoSuchConnectionException(
                                        MessageFormat.format("Did not find connection for message {0}, direction {1}",
                                                        message.getMessageURI(), direction)));
    }

    public Optional<Connection> getConnectionForMessage(WonMessage originalMessage, WonMessageDirection direction) {
        URI socketURI = null;
        URI targetSocketURI = null;
        if (direction.isFromExternal()) {
            socketURI = originalMessage.getRecipientSocketURI();
            targetSocketURI = originalMessage.getSenderSocketURI();
        } else {
            socketURI = originalMessage.getSenderSocketURI();
            targetSocketURI = originalMessage.getRecipientSocketURI();
        }
        if (targetSocketURI != null && socketURI != null) {
            return connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, targetSocketURI);
        } else {
            return Optional.empty();
        }
    }
}
