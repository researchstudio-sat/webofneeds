package won.node.socket.impl;

import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.socket.businessactivity.participantcompletion.BAPCEventType;
import won.node.socket.businessactivity.participantcompletion.BAPCState;
import won.node.socket.businessactivity.statemanager.BAStateManager;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 16.1.14. Time: 16.39 To
 * change this template use File | Settings | File Templates.
 */
public class BAPCCoordinatorSocketImpl extends AbstractBASocket {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private BAStateManager stateManager;

    @Override
    public SocketType getSocketType() {
        return SocketType.BAPCCoordinatorSocket;
    }

    public void openFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        // CONNECTED state
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.debug("*** open from Particiapnt:");
                    logger.debug("coordinator {}, participant {}", con.getAtomURI(), con.getTargetAtomURI());
                    logger.debug("con {}", con.getConnectionURI());
                    stateManager.setStateForAtomUri(BAPCState.ACTIVE.getURI(), con.getAtomURI(), con.getTargetAtomURI(),
                                    getSocketType().getURI());
                    storeBAStateForConnection(con, BAPCState.ACTIVE.getURI());
                    logger.debug("opened from Participant");
                    logger.debug("coordinator {}, participant {}", con.getAtomURI().toString(),
                                    con.getTargetAtomURI().toString());
                    logger.debug("con {}, con BAstate {}", con.getConnectionURI().toString(),
                                    stateManager.getStateForAtomUri(con.getAtomURI(), con.getTargetAtomURI(),
                                                    getSocketType().getURI()).toString());
                    // TODO: use new system
                    // ownerFacingConnectionClient.open(con.getConnectionURI(), content,
                    // wonMessage);
                } catch (Exception e) {
                    logger.warn("caught Exception:", e);
                }
            }
        });
    }

    // Coordinator sends message to Participant
    public void sendMessageFromOwner(final Connection con, final Model message, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        final URI targetConnectionURI = con.getTargetConnectionURI();
        // inform the other side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String messageForSending = new String();
                    BAPCEventType eventType = null;
                    Model myContent = null;
                    Resource r = null;
                    // message (event) for sending
                    // message as TEXT
                    messageForSending = WonRdfUtils.MessageUtils.getTextMessage(message);
                    if (messageForSending != null) {
                        eventType = BAPCEventType.getCoordinationEventTypeFromString(messageForSending);
                        logger.debug("*** Coordinator sends the text message {}", eventType);
                        logger.debug("coordinator {}, participant {}", con.getAtomURI(), con.getTargetAtomURI());
                        logger.debug("con {}", con.getConnectionURI());
                    }
                    // message as MODEL
                    else {
                        NodeIterator ni = message.listObjectsOfProperty(
                                        message.getProperty(WON_TX.COORDINATION_MESSAGE.getURI().toString()));
                        if (ni.hasNext()) {
                            String eventTypeURI = ni.toList().get(0).asResource().getURI().toString();
                            eventType = BAPCEventType.getBAEventTypeFromURI(eventTypeURI);
                            logger.debug("*** Coordinator sends the text message {}", eventType.getURI());
                            logger.debug("coordinator {}, participant {}", con.getAtomURI(), con.getTargetAtomURI());
                            logger.debug("con {}", con.getConnectionURI());
                        } else {
                            logger.debug("ERROR: Message {} does not contain a proper content.", message.toString());
                            return;
                        }
                    }
                    myContent = ModelFactory.createDefaultModel();
                    myContent.setNsPrefix("", "no:uri");
                    Resource baseResource = myContent.createResource("no:uri");
                    if ((eventType != null)) {
                        if (BAPCEventType.isBAPCCoordinatorEventType(eventType)) {
                            BAPCState state, newState;
                            state = BAPCState.parseString(stateManager.getStateForAtomUri(con.getAtomURI(),
                                            con.getTargetAtomURI(), getSocketType().getURI()).toString());
                            logger.debug("Before sending Coordinator has the BAState {} ", state.getURI().toString());
                            newState = state.transit(eventType);
                            stateManager.setStateForAtomUri(newState.getURI(), con.getAtomURI(), con.getTargetAtomURI(),
                                            getSocketType().getURI());
                            storeBAStateForConnection(con, newState.getURI());
                            logger.debug("New state of the Coordinator:" + stateManager.getStateForAtomUri(
                                            con.getAtomURI(), con.getTargetAtomURI(), getSocketType().getURI()));
                            // eventType -> URI Resource
                            r = myContent.createResource(eventType.getURI().toString());
                            baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                            // TODO: use new system
                            // atomFacingConnectionClient.sendMessage(con, myContent, wonMessage);
                        } else {
                            logger.debug("The eventType: " + eventType.getURI().toString()
                                            + " can not be triggered by Coordinator.");
                        }
                    } else {
                        logger.debug("The event type denoted by " + messageForSending + " is not allowed.");
                    }
                } catch (Exception e) {
                    logger.warn("caught Exception;", e);
                }
            }
        });
    }

    // Coordinator receives message from Participant
    public void sendMessageFromAtom(final Connection con, final Model message, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // send to the atom side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.debug("Received message from Participant: " + message.toString());
                    NodeIterator it = message.listObjectsOfProperty(WON_TX.COORDINATION_MESSAGE);
                    if (!it.hasNext()) {
                        logger.debug("message did not contain a won-ba:coordinationMessage");
                        return;
                    }
                    RDFNode coordMsgNode = it.nextNode();
                    if (!coordMsgNode.isURIResource()) {
                        logger.debug("message did not contain a won-ba:coordinationMessage URI");
                        return;
                    }
                    Resource coordMsg = coordMsgNode.asResource();
                    String sCoordMsg = coordMsg.toString(); // URI
                    // URI -> eventType
                    BAPCEventType eventType = BAPCEventType.getCoordinationEventTypeFromURI(sCoordMsg);
                    BAPCState state, newState;
                    state = BAPCState.parseString(stateManager.getStateForAtomUri(con.getAtomURI(),
                                    con.getTargetAtomURI(), getSocketType().getURI()).toString());
                    logger.debug("Current state of the Coordinator: " + state.getURI().toString());
                    newState = state.transit(eventType);
                    stateManager.setStateForAtomUri(newState.getURI(), con.getAtomURI(), con.getTargetAtomURI(),
                                    getSocketType().getURI());
                    storeBAStateForConnection(con, newState.getURI());
                    logger.debug("New state of the Coordinator:" + stateManager.getStateForAtomUri(con.getAtomURI(),
                                    con.getTargetAtomURI(), getSocketType().getURI()));
                    // TODO: use new system
                    // ownerFacingConnectionClient.sendMessage(con.getConnectionURI(), message,
                    // wonMessage);
                    BAPCEventType resendEventType = state.getResendEvent();
                    if (resendEventType != null) {
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("", "no:uri");
                        Resource baseResource = myContent.createResource("no:uri");
                        if (BAPCEventType.isBAPCCoordinatorEventType(resendEventType)) {
                            state = BAPCState.parseString(stateManager.getStateForAtomUri(con.getAtomURI(),
                                            con.getTargetAtomURI(), getSocketType().getURI()).toString());
                            logger.debug("Coordinator re-sends the previous message.");
                            logger.debug("Current state of the Coordinator: " + state.getURI().toString());
                            newState = state.transit(resendEventType);
                            stateManager.setStateForAtomUri(newState.getURI(), con.getAtomURI(), con.getTargetAtomURI(),
                                            getSocketType().getURI());
                            storeBAStateForConnection(con, newState.getURI());
                            logger.debug("New state of the Coordinator:" + stateManager.getStateForAtomUri(
                                            con.getAtomURI(), con.getTargetAtomURI(), getSocketType().getURI()));
                            // eventType -> URI Resource
                            Resource r = myContent.createResource(resendEventType.getURI().toString());
                            baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                            // baseResource.addProperty(WON_BA.COORDINATION_MESSAGE,
                            // WON_BA.COORDINATION_MESSAGE_COMMIT);
                            // TODO: use new system
                            // atomFacingConnectionClient.sendMessage(con, myContent, wonMessage);
                        } else {
                            logger.debug("The eventType: " + eventType.getURI().toString()
                                            + " can not be triggered by Participant.");
                        }
                    }
                } catch (Exception e) {
                    logger.warn("caught Exception; ", e);
                }
            }
        });
    }

    public void setStateManager(final BAStateManager stateManager) {
        this.stateManager = stateManager;
    }
}