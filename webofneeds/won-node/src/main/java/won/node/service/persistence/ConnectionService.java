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
import won.protocol.exception.NoDefaultSocketException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.SocketCapacityException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
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
import won.protocol.util.WonRdfUtils;
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

    public Optional<Connection> getConnection(URI connectionURI) {
        return connectionRepository.findOneByConnectionURI(connectionURI);
    }

    public Connection getConnectionRequired(URI connectionURI) {
        return getConnection(connectionURI).orElseThrow(() -> new NoSuchConnectionException(connectionURI));
    }

    public Connection openFromOwner(WonMessage wonMessage)
                    throws IncompatibleSocketsException, SocketCapacityException {
        wonMessage.getMessageType().requireType(WonMessageType.OPEN);
        Optional<URI> userDefinedTargetSocketURI = Optional
                        .ofNullable(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
        Optional<URI> userDefinedSocketURI = Optional.ofNullable(WonRdfUtils.SocketUtils.getSocket(wonMessage));
        URI senderURI = wonMessage.getSenderURI();
        URI senderAtomURI = wonMessage.getSenderAtomURI();
        URI recipientURI = wonMessage.getRecipientURI();
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        return openFromOwner(senderURI, senderAtomURI, recipientURI, recipientAtomURI, userDefinedSocketURI,
                        userDefinedTargetSocketURI);
    }

    public Connection openFromOwner(URI senderURI, URI senderAtomURI, URI recipientURI, URI recipientAtomURI,
                    Optional<URI> userDefinedSocketURI, Optional<URI> userDefinedTargetSocketURI)
                    throws IncompatibleSocketsException, SocketCapacityException {
        Optional<Connection> conOpt = connectionRepository.findOneByConnectionURI(senderURI);
        if (!conOpt.isPresent()) {
            throw new NoSuchConnectionException(senderURI);
        }
        Connection con = conOpt.get();
        Objects.requireNonNull(con);
        Objects.requireNonNull(con.getTargetAtomURI());
        if (!con.getTargetAtomURI().equals(recipientAtomURI))
            throw new IllegalStateException("remote atom uri must be equal to receiver atom uri");
        if (con.getConnectionURI() == null)
            throw new IllegalStateException("connection uri must not be null");
        if (con.getSocketURI() == null)
            throw new IllegalStateException("connection's socket uri must not be null");
        if (!con.getConnectionURI().equals(senderURI))
            throw new IllegalStateException("connection uri must be equal to sender uri");
        if (recipientURI != null) {
            if (!recipientURI.equals(con.getTargetConnectionURI())) {
                throw new IllegalStateException("remote connection uri must be equal to receiver uri");
            }
        }
        // sockets: the remote socket in the connection may be null before the open.
        // check if the owner sent a remote socket. there must not be a clash
        failIfIsNotSocketOfAtom(userDefinedSocketURI, Optional.of(senderAtomURI));
        failIfIsNotSocketOfAtom(userDefinedTargetSocketURI,
                        Optional.of(recipientAtomURI));
        Optional<URI> connectionsTargetSocketURI = Optional.ofNullable(con.getTargetSocketURI());
        // check remote socket info
        if (userDefinedTargetSocketURI.isPresent()) {
            if (connectionsTargetSocketURI.isPresent()) {
                if (!userDefinedTargetSocketURI.get().equals(connectionsTargetSocketURI.get())) {
                    throw new IllegalArgumentException(
                                    "Cannot process OPEN FROM_OWNER: remote socket uri clashes with value already set in connection");
                }
            } else {
                // use the one from the message
                con.setTargetSocketURI(userDefinedTargetSocketURI.get());
            }
        } else {
            // check if neither the message nor the connection have a remote socket set
            if (!connectionsTargetSocketURI.isPresent()) {
                // none defined at all: look up default remote socket
                con.setTargetSocketURI(socketService.lookupDefaultSocket(con.getTargetAtomURI())
                                .orElseThrow(() -> new NoDefaultSocketException(con.getTargetAtomURI())));
            }
        }
        failForIncompatibleSockets(con.getSocketURI(), con.getTargetSocketURI());
        ConnectionState state = con.getState();
        if (state != ConnectionState.CONNECTED) {
            state = state.transit(ConnectionEventType.OWNER_OPEN);
            if (state == ConnectionState.CONNECTED) {
                // previously unconnected connection would be established. Check capacity:
                failForExceededCapacity(con.getSocketURI());
            }
        }
        con.setState(state);
        connectionRepository.save(con);
        return con;
    }

    public Connection openFromNode(WonMessage wonMessage) {
        wonMessage.getMessageType().requireType(WonMessageType.OPEN);
        Optional<URI> connectionURIFromWonMessage = Optional.ofNullable(wonMessage.getRecipientURI());
        Optional<Connection> con = Optional.empty();
        URI senderAtomURI = wonMessage.getSenderAtomURI();
        URI senderURI = wonMessage.getSenderURI();
        URI recipientNodeURI = wonMessage.getRecipientNodeURI();
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        Optional<URI> socketURI = Optional.of(WonRdfUtils.SocketUtils.getSocket(wonMessage));
        Optional<URI> targetSocketURI = Optional.of(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
        return openFromNode(senderURI, socketURI, senderAtomURI, connectionURIFromWonMessage, targetSocketURI,
                        recipientAtomURI, recipientNodeURI);
    }

    public Connection openFromNode(URI senderURI, Optional<URI> socketURI, URI senderAtomURI,
                    Optional<URI> connectionURIFromWonMessage, Optional<URI> targetSocketURI, URI recipientAtomURI,
                    URI recipientNodeURI) {
        Optional<Connection> con;
        if (!connectionURIFromWonMessage.isPresent()) {
            // the opener didn't know about the connection
            // this happens, for example, when both parties get a hint. Both create a
            // connection, but they don't know
            // about each other.
            // That's why we first try to find a connection with the same atoms and socket:
            // let's extract the socket, we'll atom it multiple times here.
            // As the call is coming from the node, it must be present
            // (the node fills it in if the owner leaves it out)
            failIfIsNotSocketOfAtom(socketURI, Optional.of(recipientAtomURI));
            failIfIsNotSocketOfAtom(targetSocketURI, Optional.of(senderAtomURI));
            if (!socketURI.isPresent())
                throw new IllegalArgumentException(
                                "Cannot process OPEN FROM_EXTERNAl as no socket information is present");
            if (!targetSocketURI.isPresent())
                throw new IllegalArgumentException(
                                "Cannot process OPEN FROM_EXTERNAl as no remote socket information is present");
            con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(
                            recipientAtomURI, senderAtomURI, socketURI.get(),
                            targetSocketURI.get());
            if (!con.isPresent()) {
                // maybe we did not know about the targetsocket yet. let's try that:
                con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndNullTargetSocketForUpdate(
                                recipientAtomURI, senderAtomURI, socketURI.get());
            }
            if (!con.isPresent()) {
                Socket socket = socketService.getSocket(recipientAtomURI, socketURI);
                // ok, we really do not know about the connection. create it.
                URI connectionUri = wonNodeInformationService.generateConnectionURI(recipientNodeURI);
                con = Optional.of(createConnection(connectionUri, recipientAtomURI,
                                senderAtomURI, senderURI, socket.getSocketURI(),
                                socket.getTypeURI(), targetSocketURI.get(), ConnectionState.REQUEST_RECEIVED,
                                ConnectionEventType.PARTNER_OPEN));
            }
        } else {
            // the opener knew about the connection. just load it.
            con = connectionRepository.findOneByConnectionURIForUpdate(connectionURIFromWonMessage.get());
        }
        // now perform checks
        if (!con.isPresent())
            throw new IllegalStateException("connection must not be null");
        if (con.get().getTargetAtomURI() == null)
            throw new IllegalStateException("remote atom uri must not be null");
        if (!con.get().getTargetAtomURI().equals(senderAtomURI))
            throw new IllegalStateException(
                            "the remote atom uri of the connection must be equal to the sender atom uri of the message");
        if (senderURI == null)
            throw new IllegalStateException("the sender uri must not be null");
        // it is possible that we didn't store the reference to the remote conneciton
        // yet. Now we can do it.
        if (con.get().getTargetConnectionURI() == null) {
            // Set it from the message (it's the sender of the message)
            con.get().setTargetConnectionURI(senderURI);
        }
        if (!con.get().getTargetConnectionURI().equals(senderURI))
            throw new IllegalStateException("the sender uri of the message must be equal to the remote connection uri");
        failForIncompatibleSockets(con.get().getSocketURI(), con.get().getTargetSocketURI());
        ConnectionState state = con.get().getState();
        if (state != ConnectionState.CONNECTED) {
            state = state.transit(ConnectionEventType.PARTNER_OPEN);
            if (state == ConnectionState.CONNECTED) {
                // previously unconnected connection would be established. Check capacity:
                failForExceededCapacity(con.get().getSocketURI());
            }
        }
        con.get().setState(state);
        connectionRepository.save(con.get());
        return con.get();
    }

    public Connection connectFromOwner(WonMessage wonMessage)
                    throws NoSuchConnectionException, NoSuchAtomException, IllegalMessageForAtomStateException,
                    ConnectionAlreadyExistsException, SocketCapacityException, IncompatibleSocketsException {
        wonMessage.getMessageType().requireType(WonMessageType.CONNECT);
        URI senderAtomURI = wonMessage.getSenderAtomURI();
        URI senderNodeURI = wonMessage.getSenderNodeURI();
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        // this is a connect from owner. We allow owners to omit sockets for ease of
        // use.
        // If local or remote sockets were not specified, we define them now.
        Optional<URI> userDefinedSocketURI = Optional.ofNullable(WonRdfUtils.SocketUtils.getSocket(wonMessage));
        Optional<URI> userDefinedTargetSocketURI = Optional
                        .ofNullable(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
        failIfIsNotSocketOfAtom(userDefinedSocketURI, Optional.of(senderAtomURI));
        failIfIsNotSocketOfAtom(userDefinedTargetSocketURI, Optional.of(recipientAtomURI));
        Optional<URI> connectionURI = Optional.ofNullable(wonMessage.getSenderURI()); // if the uri is known already, we
                                                                                      // can
                                                                                      // load the connection!
        Connection con = connectFromOwner(senderAtomURI, senderNodeURI, recipientAtomURI,
                        userDefinedSocketURI,
                        userDefinedTargetSocketURI, connectionURI);
        return con;
    }

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
                                                .orElseGet(() -> socketService
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
                            .of(userDefinedTargetSocketURI.orElseGet(() -> socketService
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

    public Connection connectFromNode(WonMessage wonMessage) {
        // an atom wants to connect.
        // get the required data from the message and create a connection
        wonMessage.getMessageType().requireType(WonMessageType.CONNECT);
        URI atomUri = wonMessage.getRecipientAtomURI();
        URI connectionURI = wonMessage.getRecipientURI(); // if the uri is known already, we can load the connection!
        URI wonNodeUriFromWonMessage = wonMessage.getRecipientNodeURI();
        URI targetAtomUri = wonMessage.getSenderAtomURI();
        URI targetConnectionUri = wonMessage.getSenderURI();
        URI socketURI = WonRdfUtils.SocketUtils.getSocket(wonMessage);
        URI targetSocketURI = WonRdfUtils.SocketUtils.getTargetSocket(wonMessage);
        return connectFromNode(atomUri, connectionURI, socketURI, wonNodeUriFromWonMessage, targetAtomUri,
                        targetConnectionUri, targetSocketURI);
    }

    private Connection connectFromNode(URI atomUri, URI connectionURI, URI socketURI, URI wonNodeUriFromWonMessage,
                    URI targetAtomUri, URI targetConnectionUri, URI targetSocketURI) {
        if (socketURI == null) {
            throw new IllegalArgumentException("cannot process FROM_EXTERNAL connect without recipientSocketURI");
        }
        failIfIsNotSocketOfAtom(Optional.of(socketURI), Optional.of(atomUri));
        Socket socket = socketService.getSocket(atomUri, socketURI == null ? Optional.empty() : Optional.of(socketURI));
        // the remote socket must be specified in a message coming from another node
        failIfIsNotSocketOfAtom(Optional.of(targetSocketURI), Optional.of(targetAtomUri));
        // we complain about socket, not targetSocket, because it's a remote
        // message!
        if (targetSocketURI == null)
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipientSocket.toString()));
        if (targetConnectionUri == null)
            throw new MissingMessagePropertyException(URI.create(WONMSG.sender.getURI().toString()));
        Connection con = null;
        if (connectionURI != null) {
            // we already knew about this connection. load it
            con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI).get();
            if (con == null)
                throw new NoSuchConnectionException(connectionURI);
            if (con.getTargetConnectionURI() != null && !targetConnectionUri.equals(con.getTargetConnectionURI())) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_EXTERNAL. Specified connection uris conflict with existing connection data");
            }
            if (con.getTargetSocketURI() != null && !targetSocketURI.equals(con.getTargetSocketURI())) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_EXTERNAL. Specified socket uris conflict with existing connection data");
            }
        } else {
            // the sender did not know about our connection. try to find out if one exists
            // that we can use
            // we know which remote socket to connect to. There may be a connection with
            // that information already, either because the hint pointed to the remote
            // socket or because the connection is already in a different state and this
            // is a duplicate connect..
            Optional<Connection> conOpt = connectionRepository
                            .findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(atomUri,
                                            targetAtomUri, socket.getSocketURI(), targetSocketURI);
            if (conOpt.isPresent()) {
                con = conOpt.get();
            } else {
                // did not find such a connection. It could be that the connection exists, but
                // without a remote socket
                conOpt = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndNullTargetSocketForUpdate(
                                atomUri, targetAtomUri, socket.getSocketURI());
                if (conOpt.isPresent()) {
                    // we found a connection without a remote socket uri. we use this one and we'll
                    // have to set the remote socket uri.
                    con = conOpt.get();
                } else {
                    // did not find such a connection either. We can safely create a new one. (see
                    // below)
                }
            }
        }
        failForExceededCapacity(socket.getSocketURI());
        failForIncompatibleSockets(socket.getSocketURI(), targetSocketURI);
        if (con == null) {
            // create Connection in Database
            URI connectionUri = wonNodeInformationService.generateConnectionURI(wonNodeUriFromWonMessage);
            con = createConnection(connectionUri, atomUri, targetAtomUri, targetConnectionUri,
                            socket.getSocketURI(), socket.getTypeURI(), targetSocketURI,
                            ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN);
        }
        con.setTargetConnectionURI(targetConnectionUri);
        con.setTargetSocketURI(targetSocketURI);
        con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
        connectionRepository.save(con);
        return con;
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
        successResponseFromNode(senderURI, isResponseToMessageURI, responseToType);
    }

    public void successResponseFromNode(URI senderURI, URI isResponseToMessageURI, WonMessageType responseToType) {
        // only process successResponse of connect message
        if (WonMessageType.CONNECT.equals(responseToType)) {
            MessageEvent mep = this.messageEventRepository
                            .findOneByCorrespondingRemoteMessageURI(isResponseToMessageURI);
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
