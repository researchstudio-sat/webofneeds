package won.node.socket.impl;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.socket.businessactivity.coordinatorcompletion.BACCEventType;
import won.node.socket.businessactivity.coordinatorcompletion.BACCState;
import won.node.socket.businessactivity.statemanager.BAStateManager;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.WonRdfUtils;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 6.2.14. Time: 15.36 To change
 * this template use File | Settings | File Templates.
 */
public class BACCCoordinatorSocketImpl extends AbstractBASocket {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ConnectionRepository connectionRepository;
    private BAStateManager stateManager;

    @Override
    public SocketType getSocketType() {
        return SocketType.BACCCoordinatorSocket;
    }

    public void openFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        // CONNECTED state
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    stateManager.setStateForAtomUri(BACCState.ACTIVE.getURI(), con.getAtomURI(), con.getTargetAtomURI(),
                                    getSocketType().getURI());
                    storeBAStateForConnection(con, BACCState.ACTIVE.getURI());
                    // TODO: use new system
                    // ownerFacingConnectionClient.open(con.getConnectionURI(), content,
                    // wonMessage);
                    logger.debug("Coordinator state: " + stateManager.getStateForAtomUri(con.getAtomURI(),
                                    con.getTargetAtomURI(), getSocketType().getURI()));
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
                    BACCEventType eventType = null;
                    Model myContent = null;
                    Resource r = null;
                    // message (event) for sending
                    // message as TEXT
                    messageForSending = WonRdfUtils.MessageUtils.getTextMessage(message);
                    if (messageForSending != null) {
                        eventType = BACCEventType.getCoordinationEventTypeFromString(messageForSending);
                        logger.debug("Coordinator sends the text message: {}", eventType);
                    }
                    // message as MODEL
                    else {
                        NodeIterator ni = message.listObjectsOfProperty(
                                        message.getProperty(WON_TX.COORDINATION_MESSAGE.getURI().toString()));
                        if (ni.hasNext()) {
                            String eventTypeURI = ni.toList().get(0).asResource().getURI().toString();
                            eventType = BACCEventType.getBAEventTypeFromURI(eventTypeURI);
                            logger.debug("Coordinator sends the RDF: {}", eventType.getURI());
                        } else {
                            logger.debug("Message {} does not contain a proper content.", message.toString());
                            return;
                        }
                    }
                    myContent = ModelFactory.createDefaultModel();
                    myContent.setNsPrefix("", "no:uri");
                    Resource baseResource = myContent.createResource("no:uri");
                    if ((eventType != null)) {
                        if (eventType.isBACCCoordinatorEventType(eventType)) {
                            BACCState state, newState;
                            state = BACCState.parseString(stateManager.getStateForAtomUri(con.getAtomURI(),
                                            con.getTargetAtomURI(), getSocketType().getURI()).toString());
                            logger.debug("Current state of the Coordinator: " + state.getURI().toString());
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
                    logger.warn("caught Exception", e);
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
                    BACCEventType eventType = BACCEventType.getCoordinationEventTypeFromURI(sCoordMsg);
                    BACCState state, newState;
                    state = BACCState.parseString(stateManager.getStateForAtomUri(con.getAtomURI(),
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
                    BACCEventType resendEventType = state.getResendEvent();
                    if (resendEventType != null) {
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("", "no:uri");
                        Resource baseResource = myContent.createResource("no:uri");
                        if (BACCEventType.isBACCCoordinatorEventType(resendEventType)) {
                            state = BACCState.parseString(stateManager.getStateForAtomUri(con.getAtomURI(),
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
                            // TODO: use new system
                            // atomFacingConnectionClient.sendMessage(con, myContent, wonMessage);
                        } else {
                            logger.debug("The eventType: " + eventType.getURI().toString()
                                            + " can not be triggered by Participant.");
                        }
                    }
                } catch (Exception e) {
                    logger.warn("caught Exception", e);
                }
            }
        });
    }

    public void setStateManager(final BAStateManager stateManager) {
        this.stateManager = stateManager;
    }
}
