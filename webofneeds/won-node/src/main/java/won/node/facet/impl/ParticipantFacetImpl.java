package won.node.facet.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 9.12.13. Time: 19.19 To
 * change this template use File | Settings | File Templates.
 */
public class ParticipantFacetImpl extends AbstractFacet {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public FacetType getFacetType() {
        return FacetType.ParticipantFacet;
    }

    public void connectFromNeed(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        final Connection connectionForRunnable = con;
        logger.debug("Participant: ConnectFromNeed");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // TODO: use new system
                // ownerProtocolOwnerService.connect(
                // con.getNeedURI(), con.getRemoteNeedURI(),
                // connectionForRunnable.getConnectionURI(), content, wonMessage);
            }
        });
    }

    @Override
    public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the other side
        logger.debug("Participant: OpenFromOwner");
        if (con.getRemoteConnectionURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // TODO: use new system
                        // needFacingConnectionClient.open(con, content, wonMessage);
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
        if (con.getRemoteConnectionURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // TODO: use new system
                        // needFacingConnectionClient.close(con, content, wonMessage);
                    } catch (Exception e) {
                        logger.debug("caught Exception", e);
                    }
                }
            });
        }
    }

    public void closeFromNeed(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the need side
        logger.debug("Participant: CloseFromNeed");
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
                                            + con.getNeedURI() + " " + con.getRemoteNeedURI() + " " + con.getState()
                                            + " " + con.getTypeURI());
                        } else {
                            logger.debug("Committed: " + con.getConnectionURI() + " " + con.getNeedURI() + " "
                                            + con.getRemoteNeedURI() + " " + con.getState() + " " + con.getTypeURI());
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
        logger.debug("Compensated:   " + con.getConnectionURI() + " " + con.getNeedURI() + " " + con.getRemoteNeedURI()
                        + " " + con.getState() + " " + con.getTypeURI());
    }

    @Override
    public void connectFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        logger.debug("Participant: ConntectFromOwner");
        Resource baseRes = content.getResource(content.getNsPrefixURI(""));
        StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_REMOTE_FACET);
        if (!stmtIterator.hasNext())
            throw new IllegalArgumentException("at least one RDF node must be of type won:hasRemoteFacet");
        // TODO: This should just remove RemoteFacet from content and replace the value
        // of Facet with the one from RemoteFacet
        final Model remoteFacetModel = ModelFactory.createDefaultModel();
        remoteFacetModel.setNsPrefix("", "no:uri");
        baseRes = remoteFacetModel.createResource(remoteFacetModel.getNsPrefixURI(""));
        Resource remoteFacetResource = stmtIterator.next().getObject().asResource();
        baseRes.addProperty(WON.HAS_FACET, remoteFacetModel.createResource(remoteFacetResource.getURI()));
        final Connection connectionForRunnable = con;
        // send to need
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // TODO: use new system
                    // Future<URI> remoteConnectionURI = needProtocolNeedService.connect(
                    // con.getRemoteNeedURI(),con.getNeedURI(),
                    // connectionForRunnable.getConnectionURI(), remoteFacetModel, wonMessage);
                    // dataService.updateRemoteConnectionURI(con, remoteConnectionURI.get());
                } catch (Exception e) {
                    // we can't connect the connection. we send a close back to the owner
                    // TODO should we introduce a new protocol method connectionFailed (because it's
                    // not an owner deny but some protocol-level error)?
                    // For now, we call the close method as if it had been called from the remote
                    // side
                    // TODO: even with this workaround, it would be good to send a content along
                    // with the close (so we can explain what happened).
                    // try {
                    // needFacingConnectionCommunicationService.close(
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
