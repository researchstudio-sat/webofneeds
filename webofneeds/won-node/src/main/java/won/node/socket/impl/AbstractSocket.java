package won.node.socket.impl;

import java.io.StringWriter;
import java.net.URI;
import java.util.concurrent.ExecutorService;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.service.DataAccessService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 16.09.13 Time: 17:09 To
 * change this template use File | Settings | File Templates.
 */
public abstract class AbstractSocket implements SocketLogic {
    // string to be appended to the atom uri, and prepended to a socket-specific
    // identifier
    // so as to form a unique graph name used for storing data that is managed by
    // the socket
    private static final String SOCKET_GRAPH_PATH = "socketgraph";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    protected WonNodeInformationService wonNodeInformationService;
    @Autowired
    protected LinkedDataSource linkedDataSource;
    @Autowired
    protected DatasetHolderRepository datasetHolderRepository;
    @Autowired
    AtomRepository atomRepository;
    protected won.node.service.impl.URIService URIService;
    protected ExecutorService executorService;
    protected DataAccessService dataService;

    /**
     * A string that is used to create a graph URI used for atom data managed by
     * this socket.
     * 
     * @return
     */
    protected String getIdentifierForSocketManagedGraph() {
        return getClass().getSimpleName();
    }

    /**
     * This function is invoked when an owner sends an open message to a won node
     * and usually executes registered socket specific code. It is used to open a
     * connection which is identified by the connection object con. A rdf graph can
     * be sent along with the request.
     *
     * @param con     the connection object
     * @param content a rdf graph describing properties of the event. The null
     *                releative URI ('<>') inside that graph, as well as the base
     *                URI of the graph will be attached to the resource identifying
     *                the event.
     * @throws NoSuchConnectionException                 if connectionURI does not
     *                                                   refer to an existing
     *                                                   connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     *                                                   allowed in the current
     *                                                   state of the connection
     */
    @Override
    public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the other side
        // in an 'open' call the local and the remote connection URI are always known
        // and must be present
        // in the con object.
        if (wonMessage.getRecipientURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    // try {
                    // atomFacingConnectionClient.open(con, content, wonMessage);
                    // } catch (Exception e) {
                    // logger.warn("caught Exception in openFromOwner", e);
                    // }
                }
            });
        }
    }

    /**
     * This function is invoked when an owner sends a close message to a won node
     * and usually executes registered socket specific code. It is used to close a
     * connection which is identified by the connection object con. A rdf graph can
     * be sent along with the request.
     *
     * @param con     the connection object
     * @param content a rdf graph describing properties of the event. The null
     *                releative URI ('<>') inside that graph, as well as the base
     *                URI of the graph will be attached to the resource identifying
     *                the event.
     * @throws NoSuchConnectionException                 if connectionURI does not
     *                                                   refer to an existing
     *                                                   connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     *                                                   allowed in the current
     *                                                   state of the connection
     */
    @Override
    public void closeFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // //inform the other side
        // //TODO: don't inform the other side if there is none (suggested, request_sent
        // states)
        // if (con.getTargetConnectionURI() != null) {
        // executorService.execute(new Runnable()
        // {
        // @Override
        // public void run()
        // {
        // try {
        // atomFacingConnectionClient.close(con, content, wonMessage);
        // } catch (Exception e) {
        // logger.warn("caught Exception in closeFromOwner: ",e);
        // }
        // }
        // });
        // }
    }

    /**
     * This function is invoked when an owner sends a text message to a won node and
     * usually executes registered socket specific code. It is used to indicate the
     * sending of a chat message with by the specified connection object con to the
     * remote partner.
     *
     * @param con     the connection object
     * @param message the chat message
     * @throws NoSuchConnectionException                 if connectionURI does not
     *                                                   refer to an existing
     *                                                   connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     *                                                   allowed in the current
     *                                                   state of the connection
     */
    @Override
    public void sendMessageFromOwner(final Connection con, final Model message, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the other side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // atomFacingConnectionClient.sendMessage(con, message, wonMessage);
                // } catch (Exception e) {
                // logger.warn("caught Exception in textMessageFromOwner: ",e);
                // }
            }
        });
    }

    /**
     * This function is invoked when an won node sends an open message to another
     * won node and usually executes registered socket specific code. It is used to
     * open a connection which is identified by the connection object con. A rdf
     * graph can be sent along with the request.
     *
     * @param con     the connection object
     * @param content a rdf graph describing properties of the event. The null
     *                releative URI ('<>') inside that graph, as well as the base
     *                URI of the graph will be attached to the resource identifying
     *                the event.
     * @throws NoSuchConnectionException                 if connectionURI does not
     *                                                   refer to an existing
     *                                                   connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     *                                                   allowed in the current
     *                                                   state of the connection
     */
    @Override
    public void openFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // ownerFacingConnectionClient.open(wonMessage.getRecipientURI(),
                // content,
                // wonMessage);
                // } catch (Exception e) {
                // logger.warn("caught Exception in openFromAtom:", e);
                // }
            }
        });
    }

    /**
     * This function is invoked when an won node sends a close message to another
     * won node and usually executes registered socket specific code. It is used to
     * close a connection which is identified by the connection object con. A rdf
     * graph can be sent along with the request.
     *
     * @param con     the connection object
     * @param content a rdf graph describing properties of the event. The null
     *                releative URI ('<>') inside that graph, as well as the base
     *                URI of the graph will be attached to the resource identifying
     *                the event.
     * @throws NoSuchConnectionException                 if connectionURI does not
     *                                                   refer to an existing
     *                                                   connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     *                                                   allowed in the current
     *                                                   state of the connection
     */
    @Override
    public void closeFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // ownerFacingConnectionClient.close(con.getConnectionURI(), content,
                // wonMessage);
                // } catch (Exception e) {
                // logger.warn("caught Exception in closeFromAtom:", e);
                // }
            }
        });
    }

    /**
     * This function is invoked when a won node sends a text message to another won
     * node and usually executes registered socket specific code. It is used to
     * indicate the sending of a chat message with by the specified connection
     * object con to the remote partner.
     *
     * @param con     the connection object
     * @param message the chat message
     * @throws NoSuchConnectionException                 if connectionURI does not
     *                                                   refer to an existing
     *                                                   connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     *                                                   allowed in the current
     *                                                   state of the connection
     */
    @Override
    public void sendMessageFromAtom(final Connection con, final Model message, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // send to the atom side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // ownerFacingConnectionClient.sendMessage(con.getConnectionURI(), message,
                // wonMessage);
                // } catch (Exception e) {
                // logger.warn("caught Exception in textMessageFromAtom:", e);
                // }
            }
        });
    }

    /**
     * This function is invoked when a matcher sends a hint message to a won node
     * and usually executes registered socket specific code. It notifies the atom of
     * a matching otherAtom with the specified match score. Originator identifies
     * the entity making the call. Normally, originator is a matching service. A rdf
     * graph can be sent along with the request.
     *
     * @param con        the connection object
     * @param score      match score between 0.0 (bad) and 1.0 (good).
     *                   Implementations treat lower values as 0.0 and higher values
     *                   as 1.0.
     * @param originator an URI identifying the calling entity
     * @param content    (optional) an optional RDF graph containing more detailed
     *                   information about the hint. The null releative URI ('<>')
     *                   inside that graph, as well as the base URI of the graph
     *                   will be attached to the resource identifying the match
     *                   event.
     * @throws won.protocol.exception.NoSuchAtomException                 if atomURI
     *                                                                    is not a
     *                                                                    known atom
     *                                                                    URI
     * @throws won.protocol.exception.IllegalMessageForAtomStateException if the
     *                                                                    atom is
     *                                                                    not active
     */
    @Override
    public void hint(final Connection con, final double score, final URI originator, final Model content,
            final WonMessage wonMessage) throws NoSuchAtomException, IllegalMessageForAtomStateException {
        Model targetSocketModelCandidate = content;
        if (wonMessage == null)
            targetSocketModelCandidate = changeHasTargetSocketToHasSocket(content);
        final Model targetSocketModel = targetSocketModelCandidate;
        try {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    // here, we don't really need to handle exceptions, as we don't want to flood
                    // matching services with error messages
                    // try {
                    // ownerProtocolOwnerService.hint(
                    // con.getAtomURI(), con.getTargetAtomURI(),
                    // score, originator, targetSocketModel, wonMessage);
                    // } catch (NoSuchAtomException e) {
                    // logger.warn("error sending hint message to owner - no such atom:", e);
                    // } catch (IllegalMessageForAtomStateException e) {
                    // logger.warn("error sending hint content to owner - illegal atom state:", e);
                    // } catch (Exception e) {
                    // logger.warn("error sending hint content to owner:", e);
                    // }
                }
            });
        } catch (WonMessageBuilderException e) {
            logger.warn("error creating HintMessage", e);
        }
    }

    /**
     * This function is invoked when an won node sends an connect message to another
     * won node and usually executes registered socket specific code. The connection
     * is identified by the connection object con. A rdf graph can be sent along
     * with the request.
     *
     * @param con     the connection object
     * @param content a rdf graph describing properties of the event. The null
     *                releative URI ('<>') inside that graph, as well as the base
     *                URI of the graph will be attached to the resource identifying
     *                the event.
     * @throws NoSuchConnectionException                 if connectionURI does not
     *                                                   refer to an existing
     *                                                   connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     *                                                   allowed in the current
     *                                                   state of the connection
     */
    @Override
    public void connectFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
            throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        final Connection connectionForRunnable = con;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // ownerProtocolOwnerService.connect(con.getAtomURI(), con.getTargetAtomURI(),
                // connectionForRunnable.getConnectionURI(), content, wonMessage);
                // } catch (WonProtocolException e) {
                // // we can't connect the connection. we send a deny back to the owner
                // // TODO should we introduce a new protocol method connectionFailed (because
                // it's not an owner deny but some protocol-level error)?
                // // For now, we call the close method as if it had been called from the owner
                // side
                // // TODO: even with this workaround, it would be good to send a content along
                // with the close (so we can explain what happened).
                // logger.warn("could not connectFromAtom, sending close back. Exception was:
                // ",e);
                //// try {
                //// // ToDo
                //// ownerFacingConnectionCommunicationService.close(
                //// connectionForRunnable.getConnectionURI(), content, wonMessage);
                //// } catch (Exception e1) {
                //// logger.warn("caught Exception sending close back from connectFromAtom:",
                // e1);
                //// }
                // }
            }
        });
    }

    /**
     * This function is invoked when an owner sends an open message to the won node
     * and usually executes registered socket specific code. The connection is
     * identified by the connection object con. A rdf graph can be sent along with
     * the request.
     *
     * @param con     the connection object
     * @param content a rdf graph describing properties of the event. The null
     *                releative URI ('<>') inside that graph, as well as the base
     *                URI of the graph will be attached to the resource identifying
     *                the event.
     * @throws NoSuchConnectionException                 if connectionURI does not
     *                                                   refer to an existing
     *                                                   connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     *                                                   allowed in the current
     *                                                   state of the connection
     */
    @Override
    public void connectFromOwner(final Connection con, final Model content, WonMessage wonMessage)
            throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        Model targetSocketModel = null;
        targetSocketModel = changeHasTargetSocketToHasSocket(content);
        final Connection connectionForRunnable = con;
        // send to atom
        // try {
        // final ListenableFuture<URI> targetConnectionURI =
        // atomProtocolAtomService.connect(con.getTargetAtomURI(),
        // con.getAtomURI(), connectionForRunnable.getConnectionURI(),
        // targetSocketModel,
        // wonMessage);
        // this.executorService.execute(new Runnable(){
        // @Override
        // public void run() {
        // try{
        // if (logger.isDebugEnabled()) {
        // logger.debug("saving remote connection URI");
        // }
        // dataService.updateTargetConnectionURI(con, targetConnectionURI.get());
        // } catch (Exception e) {
        // logger.warn("Error saving connection {}. Stacktrace follows", con);
        // logger.warn("Error saving connection ", e);
        // }
        // }
        // });
        //
        // } catch (WonProtocolException e) {
        // // we can't connect the connection. we send a close back to the owner
        // // TODO should we introduce a new protocol method connectionFailed (because
        // it's not an owner deny but some protocol-level error)?
        // // For now, we call the close method as if it had been called from the remote
        // side
        // // TODO: even with this workaround, it would be good to send a content along
        // with the close (so we can explain what happened).
        // logger.warn("could not connectFromOwner, sending close back. Exception was:
        // ",e);
        // try {
        //
        // // this WonMessage is not valid (the sender part) since it should be send
        // from the remote WON node
        // // but it should be replaced with a response message anyway
        // WonMessageBuilder builder = new WonMessageBuilder();
        // WonMessage closeWonMessage = builder
        // .setMessageURI(wonNodeInformationService.generateEventURI(
        // wonMessage.getSenderNodeURI())) //not sure if this is correct
        // .setWonMessageType(WonMessageType.CLOSE)
        // .setSenderURI(wonMessage.getSenderURI())
        // .setSenderAtomURI(wonMessage.getSenderAtomURI())
        // .setSenderNodeURI(wonMessage.getSenderNodeURI())
        // .setRecipientURI(wonMessage.getSenderURI())
        // .setRecipientAtomURI(wonMessage.getSenderAtomURI())
        // .setRecipientNodeURI(wonMessage.getSenderNodeURI())
        // .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
        // .build();
        //
        //// atomFacingConnectionCommunicationService.close(
        //// connectionForRunnable.getConnectionURI(), content, closeWonMessage);
        // } catch (Exception e1) {
        // logger.warn("caught Exception sending close back from connectFromOwner::",
        // e1);
        // }
        // } catch (Exception e) {
        // logger.warn("caught Exception in connectFromOwner: ",e);
        // }
    }

    /**
     * The socket may store data into the atom dataset; this method finds the graph
     * that the socket may write to and creates a new Graph in the dataset if
     * needed.
     * 
     * @param atomURI
     * @return the socket-managed graph
     */
    protected Model getSocketManagedGraph(URI atomURI, Dataset atomDataset) {
        String graphURI = getSocketManagedGraphName(atomURI);
        Model socketManagedGraph = atomDataset.getNamedModel(graphURI);
        if (socketManagedGraph == null) {
            socketManagedGraph = ModelFactory.createDefaultModel();
            atomDataset.addNamedModel(graphURI, socketManagedGraph);
        }
        return socketManagedGraph;
    }

    /**
     * The socket may store data into the atom dataset; this method removes that
     * data from the atom dataset.
     * 
     * @param atomURI
     */
    protected void removeSocketManagedGraph(URI atomURI, Dataset atomDataset) {
        String graphURI = getSocketManagedGraphName(atomURI);
        Model socketManagedGraph = atomDataset.getNamedModel(graphURI);
        if (socketManagedGraph != null) {
            atomDataset.removeNamedModel(graphURI);
        }
    }

    private String getSocketManagedGraphName(final URI atomURI) {
        // TODO: these checks can be made mor throrough once graph signing is in place
        return atomURI.toString() + "/" + SOCKET_GRAPH_PATH + getIdentifierForSocketManagedGraph();
    }

    /**
     * Creates a copy of the specified model, replacing won:targetSocket by
     * won:socket and vice versa.
     * 
     * @param model
     * @return
     */
    private Model changeHasTargetSocketToHasSocket(Model model) {
        Resource baseRes = model.getResource(model.getNsPrefixURI(""));
        StmtIterator stmtIterator = baseRes.listProperties(WON.targetSocket);
        if (!stmtIterator.hasNext())
            throw new IllegalArgumentException("at least one socket must be specified with won:targetSocket");
        final Model newModel = ModelFactory.createDefaultModel();
        newModel.setNsPrefix("", model.getNsPrefixURI(""));
        newModel.add(model);
        newModel.removeAll(null, WON.targetSocket, null);
        newModel.removeAll(null, WON.socket, null);
        Resource newBaseRes = newModel.createResource(newModel.getNsPrefixURI(""));
        // replace won:socket
        while (stmtIterator.hasNext()) {
            Resource socket = stmtIterator.nextStatement().getObject().asResource();
            newBaseRes.addProperty(WON.socket, socket);
        }
        // replace won:targetSocket
        stmtIterator = baseRes.listProperties(WON.socket);
        if (stmtIterator != null) {
            while (stmtIterator.hasNext()) {
                Resource socket = stmtIterator.nextStatement().getObject().asResource();
                newBaseRes.addProperty(WON.targetSocket, socket);
            }
        }
        if (logger.isDebugEnabled()) {
            StringWriter modelAsString = new StringWriter();
            RDFDataMgr.write(modelAsString, model, Lang.TTL);
            StringWriter newModelAsString = new StringWriter();
            RDFDataMgr.write(newModelAsString, model, Lang.TTL);
            logger.debug("changed targetSocket to socket. Old: \n{},\n new: \n{}", modelAsString.toString(),
                    newModelAsString.toString());
        }
        return newModel;
    }

    private boolean isAtomActive(final Atom atom) {
        return AtomState.ACTIVE == atom.getState();
    }

    public void setDataService(DataAccessService dataService) {
        this.dataService = dataService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setURIService(won.node.service.impl.URIService URIService) {
        this.URIService = URIService;
    }
}
