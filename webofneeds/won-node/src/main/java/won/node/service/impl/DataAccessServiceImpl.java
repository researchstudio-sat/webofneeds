package won.node.service.impl;

import java.lang.invoke.MethodHandles;
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
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.model.Atom;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionMessageContainer;
import won.protocol.model.ConnectionState;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageContainer;
import won.protocol.model.MessageEvent;
import won.protocol.model.Socket;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONCON;

/**
 * T User: gabriel Date: 06/11/13
 */
public class DataAccessServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private SocketRepository socketRepository;
    @Autowired
    protected ConnectionContainerRepository connectionContainerRepository;
    @Autowired
    protected AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    protected ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    protected DatasetHolderRepository datasetHolderRepository;
    @Autowired
    private MessageEventRepository messageEventRepository;

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

    /**
     * If we are saving response message, update original massage with the
     * information about response message uri
     * 
     * @param message response message
     */
    public void updateResponseInfo(final WonMessage message) {
        // find a message it responds to
        URI originalMessageURI = message.getIsResponseToMessageURI();
        if (originalMessageURI != null) {
            // update the message it responds to with the uri of the response
            messageEventRepository.lockConnectionAndMessageContainerByContainedMessageForUpdate(originalMessageURI);
            messageEventRepository.lockAtomAndMessageContainerByContainedMessageForUpdate(originalMessageURI);
            MessageEvent event = messageEventRepository.findOneByMessageURIforUpdate(originalMessageURI);
            if (event != null) {
                // we may not have saved the event yet if the current message is a
                // FailureResponse
                // and the error causing the response happened before saving the original
                // message.
                event.setResponseMessageURI(message.getMessageURI());
                messageEventRepository.save(event);
            }
        }
    }

    public void saveMessage(final WonMessage wonMessage, URI parent) {
        logger.debug("STORING message with uri {} and parent uri", wonMessage.getMessageURI(), parent);
        MessageContainer container = loadOrCreateMessageContainer(parent, wonMessage.getMessageType());
        DatasetHolder datasetHolder = new DatasetHolder(wonMessage.getMessageURI(),
                        WonMessageEncoder.encodeAsDataset(wonMessage));
        MessageEvent event = new MessageEvent(parent, wonMessage, container);
        event.setDatasetHolder(datasetHolder);
        messageEventRepository.save(event);
    }

    public MessageContainer loadOrCreateMessageContainer(final URI parent, final WonMessageType messageType) {
        if (WonMessageType.CREATE_ATOM.equals(messageType)) {
            // create an atom event container with null parent (because it will only be
            // persisted at a later point in time)
            MessageContainer container = atomMessageContainerRepository.findOneByParentUriForUpdate(parent);
            if (container != null)
                return container;
            AtomMessageContainer nec = new AtomMessageContainer(null, parent);
            atomMessageContainerRepository.saveAndFlush(nec);
            return nec;
        } else if (WonMessageType.CONNECT.equals(messageType)
                        || WonMessageType.SOCKET_HINT_MESSAGE.equals(messageType)) {
            // create a connection event container witn null parent (because it will only be
            // persisted at a later point in
            // time)
            MessageContainer container = connectionMessageContainerRepository.findOneByParentUriForUpdate(parent);
            if (container != null)
                return container;
            ConnectionMessageContainer cec = new ConnectionMessageContainer(null, parent);
            connectionMessageContainerRepository.saveAndFlush(cec);
            return cec;
        }
        MessageContainer container = atomMessageContainerRepository.findOneByParentUriForUpdate(parent);
        if (container != null)
            return container;
        container = connectionMessageContainerRepository.findOneByParentUriForUpdate(parent);
        if (container != null)
            return container;
        // let's see if we can find the event conta
        throw new IllegalArgumentException("Cannot store '" + messageType + "' event: unable to find "
                        + "event container with parent URI '" + parent + "'");
    }
}
