package won.node.socket.impl;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.SocketType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 9.12.13. Time: 19.20 To
 * change this template use File | Settings | File Templates.
 */
public class CoordinatorSocketImpl extends AbstractSocket {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public SocketType getSocketType() {
        return SocketType.CoordinatorSocket;
    }

    @Override
    public void connectFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        logger.debug("Coordinator: ConntectFromOwner");
        Resource baseRes = content.getResource(content.getNsPrefixURI(""));
        StmtIterator stmtIterator = baseRes.listProperties(WON.targetSocket);
        if (!stmtIterator.hasNext())
            throw new IllegalArgumentException("at least one RDF node must be of type won:targetSocket");
        // TODO: This should just remove TargetSocket from content and replace the value
        // of Socket with the one from TargetSocket
        final Model targetSocketModel = ModelFactory.createDefaultModel();
        targetSocketModel.setNsPrefix("", "no:uri");
        baseRes = targetSocketModel.createResource(targetSocketModel.getNsPrefixURI(""));
        Resource targetSocketResource = stmtIterator.next().getObject().asResource();
        baseRes.addProperty(WON.socket, targetSocketModel.createResource(targetSocketResource.getURI()));
        final Connection connectionForRunnable = con;
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
        // } catch (WonProtocolException e) {
        // // we can't connect the connection. we send a close back to the owner
        // // TODO should we introduce a new protocol method connectionFailed (because
        // it's not an owner deny but some protocol-level error)?
        // // For now, we call the close method as if it had been called from the remote
        // side
        // // TODO: even with this workaround, it would be good to send a content along
        // with the close (so we can explain what happened).
        //// try {
        //// Connection c = closeConnectionLocally(connectionForRunnable, content);
        //// // ToDo (FS): should probably not be the same wonMessage!?
        //// atomFacingConnectionCommunicationService.close(c.getConnectionURI(),
        // content, wonMessage);
        //// } catch (NoSuchConnectionException e1) {
        //// logger.warn("caught NoSuchConnectionException:", e1);
        //// } catch (IllegalMessageForConnectionStateException e1) {
        //// logger.warn("caught IllegalMessageForConnectionStateException:", e1);
        //// }
        // }
        // catch (InterruptedException e) {
        // e.printStackTrace(); //To change body of catch statement use File | Settings
        // | File Templates.
        // } catch (ExecutionException e) {
        // e.printStackTrace(); //To change body of catch statement use File | Settings
        // | File Templates.
        // } catch (Exception e) {
        // logger.debug("caught Exception", e);
        // }
    }

    public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the other side
        logger.debug("Coordinator: OpenFromOwner");
        if (con.getTargetConnectionURI() != null) {
            // executorService.execute(new Runnable() {
            // @Override
            // public void run() {
            // try {
            // atomFacingConnectionClient.open(con, content, wonMessage);
            // } catch (WonProtocolException e) {
            // logger.debug("caught Exception:", e);
            // } catch (Exception e) {
            // logger.debug("caught Exception", e);
            // }
            // }
            // });
        }
    }

    public void openFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        logger.debug("Coordinator: OpenFromAtom");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // ownerFacingConnectionClient.open(con.getConnectionURI(), content);
                List<Connection> cons = connectionRepository.findByAtomURIAndStateAndTypeURI(con.getAtomURI(),
                                ConnectionState.REQUEST_SENT, SocketType.CoordinatorSocket.getURI());
                boolean fAllVotesReceived = cons.isEmpty();
                if (fAllVotesReceived) {
                    Model myContent = ModelFactory.createDefaultModel();
                    myContent.setNsPrefix("", "no:uri");
                    Resource baseResource = myContent.createResource("no:uri");
                    baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_COMMIT);
                    // TODO: use new system
                    // ownerFacingConnectionClient.open(con.getConnectionURI(), myContent,
                    // wonMessage);
                    globalCommit(con);
                } else {
                    logger.debug("Wait for votes of: ");
                    for (Connection c : cons) {
                        logger.debug("   " + c.getConnectionURI() + " " + c.getAtomURI() + " " + c.getTargetAtomURI());
                    }
                    // TODO: use new system
                    // ownerFacingConnectionClient.open(con.getConnectionURI(), content,
                    // wonMessage);
                }
                // } catch (WonProtocolException e) {
                // logger.debug("caught Exception:", e);
                // }
            }
        });
    }

    @Override
    public void closeFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        logger.debug("Coordinator: closeFromOwner");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // try {
                // TODO: use new system
                // ownerFacingConnectionClient.close(con.getConnectionURI(), content,
                // wonMessage);
                globalAbort(con);
                // } catch (WonProtocolException e) {
                // logger.warn("caught WonProtocolException:", e);
                // }
            }
        });
    }

    private void globalAbort(Connection con) {
        Model myContent = ModelFactory.createDefaultModel();
        myContent.setNsPrefix("", "no:uri");
        Resource baseResource = myContent.createResource("no:uri");
        baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_ABORT);
        abortTransaction(con, myContent);
    }

    public void globalCommit(Connection con) {
        Model myContent = ModelFactory.createDefaultModel();
        myContent.setNsPrefix("", "no:uri");
        Resource baseResource = myContent.createResource("no:uri");
        baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_COMMIT);
        commitTransaction(con, myContent);
    }

    public void abortTransaction(Connection con, Model content) {
        List<Connection> cons = connectionRepository.findByAtomURI(con.getAtomURI());
        try {
            for (Connection c : cons) {
                if (c.getState() != ConnectionState.CLOSED) {
                    Model myContent = ModelFactory.createDefaultModel();
                    myContent.setNsPrefix("", "no:uri");
                    Resource res = myContent.createResource("no:uri");
                    if (c.getState() == ConnectionState.CONNECTED || c.getState() == ConnectionState.REQUEST_SENT) {
                        if (res == null) {
                            logger.debug("no default prexif specified in model, could not obtain additional content, using ABORTED message");
                        }
                        res.removeAll(WON_TX.COORDINATION_MESSAGE);
                        res.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_ABORT);
                    }
                    // todo: use new system
                    // closeConnectionLocally(c, content);
                    // atomFacingConnectionClient.close(c, myContent, null); //Abort sent to
                    // participant
                }
            }
            // } catch (WonProtocolException e) {
            // logger.warn("caught WonProtocolException:", e);
        } catch (Exception e) {
            logger.debug("caught Exception", e);
        }
    }

    public void commitTransaction(Connection con, Model content) {
        List<Connection> cons = connectionRepository.findByAtomURIAndStateAndTypeURI(con.getAtomURI(),
                        ConnectionState.CONNECTED, SocketType.CoordinatorSocket.getURI());
        try {
            for (Connection c : cons) {
                if (c.getState() == ConnectionState.CONNECTED) {
                    // emulate a close by owner
                    // todo: use new system
                    // c = closeConnectionLocally(c, content);
                    // tell the partner
                    // atomFacingConnectionClient.close(c, content, null);
                }
            }
            logger.debug("Transaction commited!");
            // } catch (WonProtocolException e) {
            // logger.warn("caught WonProtocolException:", e);
        } catch (Exception e) {
            logger.debug("caught Exception", e);
        }
    }
}
