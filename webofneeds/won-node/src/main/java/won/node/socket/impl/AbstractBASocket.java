package won.node.socket.impl;

import java.io.StringWriter;
import java.net.URI;
import java.util.concurrent.ExecutorService;

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
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

/**
 * User: Danijel Date: 4.6.14.
 */
public abstract class AbstractBASocket implements SocketLogic {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected won.node.service.impl.URIService URIService;
    protected ExecutorService executorService;
    protected DataAccessService dataService;
    @Autowired
    protected DatasetHolderRepository datasetHolderRepository;

    /**
     * This function is invoked when an owner sends an open message to a won node
     * and usually executes registered socket specific code. It is used to open a
     * connection which is identified by the connection object con. A rdf graph can
     * be sent along with the request.
     *
     * @param con the connection object
     * @param content a rdf graph describing properties of the event. The null
     * releative URI ('<>') inside that graph, as well as the base URI of the graph
     * will be attached to the resource identifying the event.
     * @throws won.protocol.exception.NoSuchConnectionException if connectionURI
     * does not refer to an existing connection
     * @throws won.protocol.exception.IllegalMessageForConnectionStateException if
     * the message is not allowed in the current state of the connection
     */
    @Override
    public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the other side
        if (con.getTargetConnectionURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    // try {
                    // atomFacingConnectionClient.open(con, content, wonMessage);
                    // } catch (Exception e) {
                    // logger.warn("caught Exception in openFromOwner",e);
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
     * @param con the connection object
     * @param content a rdf graph describing properties of the event. The null
     * releative URI ('<>') inside that graph, as well as the base URI of the graph
     * will be attached to the resource identifying the event.
     * @throws NoSuchConnectionException if connectionURI does not refer to an
     * existing connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     * allowed in the current state of the connection
     */
    @Override
    public void closeFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the other side
        if (con.getTargetConnectionURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    // try {
                    // atomFacingConnectionClient.close(con, content, wonMessage);
                    // } catch (Exception e) {
                    // logger.warn("caught Exception in closeFromOwner: ",e);
                    // }
                }
            });
        }
    }

    /**
     * This function is invoked when an won node sends an open message to another
     * won node and usually executes registered socket specific code. It is used to
     * open a connection which is identified by the connection object con. A rdf
     * graph can be sent along with the request.
     *
     * @param con the connection object
     * @param content a rdf graph describing properties of the event. The null
     * releative URI ('<>') inside that graph, as well as the base URI of the graph
     * will be attached to the resource identifying the event.
     * @throws NoSuchConnectionException if connectionURI does not refer to an
     * existing connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     * allowed in the current state of the connection
     */
    @Override
    public void openFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // ownerFacingConnectionClient.open(con.getConnectionURI(), content,
                    // wonMessage);
                } catch (Exception e) {
                    logger.warn("caught Exception in openFromAtom:", e);
                }
            }
        });
    }

    /**
     * This function is invoked when an won node sends a close message to another
     * won node and usually executes registered socket specific code. It is used to
     * close a connection which is identified by the connection object con. A rdf
     * graph can be sent along with the request.
     *
     * @param con the connection object
     * @param content a rdf graph describing properties of the event. The null
     * releative URI ('<>') inside that graph, as well as the base URI of the graph
     * will be attached to the resource identifying the event.
     * @throws NoSuchConnectionException if connectionURI does not refer to an
     * existing connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     * allowed in the current state of the connection
     */
    @Override
    public void closeFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // ownerFacingConnectionClient.close(con.getConnectionURI(), content,
                    // wonMessage);
                } catch (Exception e) {
                    logger.warn("caught Exception in closeFromAtom:", e);
                }
            }
        });
    }

    public void storeBAStateForConnection(Connection con, URI stateUri) {
        Model connectionBAStateContent = ModelFactory.createDefaultModel();
        connectionBAStateContent.setNsPrefix("", con.getConnectionURI().toString());
        Resource baseResource = connectionBAStateContent.createResource(con.getConnectionURI().toString());
        baseResource.addProperty(WON_TX.BA_STATE, connectionBAStateContent.createResource(stateUri.toString()));
        logger.debug("linked data:" + RdfUtils.toString(connectionBAStateContent));
        con.getDatasetHolder().getDataset().setDefaultModel(connectionBAStateContent);
        datasetHolderRepository.save(con.getDatasetHolder());
    }

    /**
     * This function is invoked when a matcher sends a hint message to a won node
     * and usually executes registered socket specific code. It notifies the atom of
     * a matching otherAtom with the specified match score. Originator identifies
     * the entity making the call. Normally, originator is a matching service. A rdf
     * graph can be sent along with the request.
     *
     * @param con the connection object
     * @param score match score between 0.0 (bad) and 1.0 (good). Implementations
     * treat lower values as 0.0 and higher values as 1.0.
     * @param originator an URI identifying the calling entity
     * @param content (optional) an optional RDF graph containing more detailed
     * information about the hint. The null releative URI ('<>') inside that graph,
     * as well as the base URI of the graph will be attached to the resource
     * identifying the match event.
     * @throws won.protocol.exception.NoSuchAtomException if atomURI is not a known
     * atom URI
     * @throws won.protocol.exception.IllegalMessageForAtomStateException if the
     * atom is not active
     */
    @Override
    public void hint(final Connection con, final double score, final URI originator, final Model content,
                    final WonMessage wonMessage) throws NoSuchAtomException, IllegalMessageForAtomStateException {
        final Model targetSocketModel = changeHasTargetSocketToHasSocket(content);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // here, we don't really atom to handle exceptions, as we don't want to flood
                // matching services with error messages
                // try {
                //// ownerProtocolOwnerService.hint(
                //// con.getAtomURI(),
                //// con.getTargetAtomURI(),
                //// score,
                //// originator,
                //// targetSocketModel,
                //// wonMessage);
                // } catch (NoSuchAtomException e) {
                // logger.warn("error sending hint message to owner - no such atom:", e);
                // } catch (IllegalMessageForAtomStateException e) {
                // logger.warn("error sending hint content to owner - illegal atom state:", e);
                // } catch (Exception e) {
                // logger.warn("error sending hint content to owner:", e);
                // }
            }
        });
    }

    /**
     * This function is invoked when an won node sends an connect message to another
     * won node and usually executes registered socket specific code. The connection
     * is identified by the connection object con. A rdf graph can be sent along
     * with the request.
     *
     * @param con the connection object
     * @param content a rdf graph describing properties of the event. The null
     * releative URI ('<>') inside that graph, as well as the base URI of the graph
     * will be attached to the resource identifying the event.
     * @throws NoSuchConnectionException if connectionURI does not refer to an
     * existing connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     * allowed in the current state of the connection
     */
    @Override
    public void connectFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        final Connection connectionForRunnable = con;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // ownerProtocolOwnerService.connect(
                // con.getAtomURI(),
                // con.getTargetAtomURI(),
                // connectionForRunnable.getConnectionURI(),
                // content,
                // wonMessage);
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
                //// // ToDo (FS): in this case a close wonMessage should be created and send
                // instead!!
                //// ownerFacingConnectionCommunicationService.close(
                //// connectionForRunnable.getConnectionURI(),
                //// content,
                //// wonMessage);
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
     * @param con the connection object
     * @param content a rdf graph describing properties of the event. The null
     * releative URI ('<>') inside that graph, as well as the base URI of the graph
     * will be attached to the resource identifying the event.
     * @throws NoSuchConnectionException if connectionURI does not refer to an
     * existing connection
     * @throws IllegalMessageForConnectionStateException if the message is not
     * allowed in the current state of the connection
     */
    @Override
    public void connectFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        final Model targetSocketModel = changeHasTargetSocketToHasSocket(content);
        final Connection connectionForRunnable = con;
        // send to atom
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // ListenableFuture<URI> targetConnectionURI = atomProtocolAtomService.connect(
                // con.getTargetAtomURI(),
                // con.getAtomURI(),
                // connectionForRunnable.getConnectionURI(),
                // targetSocketModel,
                // wonMessage);
                // dataService.updateTargetConnectionURI(con, targetConnectionURI.get());
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
                //// try {
                //// atomFacingConnectionCommunicationService.close(
                //// connectionForRunnable.getConnectionURI(),
                //// content,
                //// wonMessage);
                //// } catch (Exception e1) {
                //// logger.warn("caught Exception sending close back from connectFromOwner::",
                // e1);
                //// }
                // } catch (Exception e) {
                // logger.warn("caught Exception in connectFromOwner: ",e);
                // }
            }
        });
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
