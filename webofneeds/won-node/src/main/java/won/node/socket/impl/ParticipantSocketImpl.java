package won.node.socket.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;

import java.lang.invoke.MethodHandles;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 9.12.13. Time: 19.19 To
 * change this template use File | Settings | File Templates.
 */
public class ParticipantSocketImpl extends AbstractSocket {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public SocketType getSocketType() {
        return SocketType.ParticipantSocket;
    }

    public void connectFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        final Connection connectionForRunnable = con;
        logger.debug("Participant: ConnectFromAtom");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // TODO: use new system
                // ownerProtocolOwnerService.connect(
                // con.getAtomURI(), con.getTargetAtomURI(),
                // connectionForRunnable.getConnectionURI(), content, wonMessage);
            }
        });
    }

    @Override
    public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the other side
        logger.debug("Participant: OpenFromOwner");
        if (con.getTargetConnectionURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // TODO: use new system
                        // atomFacingConnectionClient.open(con, content, wonMessage);
                    } catch (Exception e) {
                        logger.debug("caught Exception", e);
                    }
                }
            });
        }
    }

    @Override
    public void closeFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the other side
        logger.debug("Participant: CloseFromOwner");
        if (con.getTargetConnectionURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // TODO: use new system
                        // atomFacingConnectionClient.close(con, content, wonMessage);
                    } catch (Exception e) {
                        logger.debug("caught Exception", e);
                    }
                }
            });
        }
    }

    public void closeFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        logger.debug("Participant: CloseFromAtom");
        // TODO: create utilities for accessing addtional content
        Resource res = content.getResource(content.getNsPrefixURI(""));
        Resource message = null;
        if (res == null) {
            logger.debug("no default prexif specified in model, could not obtain additional content, using ABORTED message");
            message = WON_TX.COORDINATION_MESSAGE_ABORT;
        }
        // TODO: make sure there is only one message in the content
        message = res.getPropertyResourceValue(WON_TX.COORDINATION_MESSAGE);
        final Resource msgForRunnable = message;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // if (msgForRunnable == WON_TX.COORDINATION_MESSAGE_ABORT){
                    if (msgForRunnable != null) {
                        if (msgForRunnable.equals(WON_TX.COORDINATION_MESSAGE_ABORT)) {
                            logger.debug("Abort the following connection: " + con.getConnectionURI() + " "
                                            + con.getAtomURI() + " " + con.getTargetAtomURI() + " " + con.getState()
                                            + " " + con.getTypeURI());
                        } else {
                            logger.debug("Committed: " + con.getConnectionURI() + " " + con.getAtomURI() + " "
                                            + con.getTargetAtomURI() + " " + con.getState() + " " + con.getTypeURI());
                        }
                        // TODO: use new system
                        // ownerFacingConnectionClient.close(con.getConnectionURI(), content,
                        // wonMessage);
                    }
                } catch (Exception e) {
                    logger.warn("caught WonProtocolException:", e);
                }
            }
        });
    }

    public void compensate(Connection con, Model content) { // todo
        // TODO: create utilities for accessing addtional content
        /*
         * Resource res = content.getResource(content.getNsPrefixURI("")); if (res ==
         * null) { logger.
         * debug("no default prexif specified in model, could not obtain additional content, using ABORTED message"
         * ); } res.removeAll(WON_TX.COORDINATION_MESSAGE);
         * res.addProperty(WON_TX.COORDINATION_MESSAGE,
         * WON_TX.COORDINATION_MESSAGE_ABORT_AND_COMPENSATE);
         */
        logger.debug("Compensated:   " + con.getConnectionURI() + " " + con.getAtomURI() + " " + con.getTargetAtomURI()
                        + " " + con.getState() + " " + con.getTypeURI());
    }

    @Override
    public void connectFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        logger.debug("Participant: ConntectFromOwner");
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
        // send to atom
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // TODO: use new system
                    // Future<URI> targetConnectionURI = atomProtocolAtomService.connect(
                    // con.getTargetAtomURI(),con.getAtomURI(),
                    // connectionForRunnable.getConnectionURI(), targetSocketModel, wonMessage);
                    // dataService.updateTargetConnectionURI(con, targetConnectionURI.get());
                } catch (Exception e) {
                    // we can't connect the connection. we send a close back to the owner
                    // TODO should we introduce a new protocol method connectionFailed (because it's
                    // not an owner deny but some protocol-level error)?
                    // For now, we call the close method as if it had been called from the remote
                    // side
                    // TODO: even with this workaround, it would be good to send a content along
                    // with the close (so we can explain what happened).
                    // try {
                    // atomFacingConnectionCommunicationService.close(
                    // connectionForRunnable.getConnectionURI(), content, wonMessage);
                    // } catch (NoSuchConnectionException e1) {
                    // logger.warn("caught NoSuchConnectionException:", e1);
                    // } catch (IllegalMessageForConnectionStateException e1) {
                    // logger.warn("caught IllegalMessageForConnectionStateException:", e1);
                    // }
                }
            }
        });
    }
}
