package won.node.socket.impl;

import java.net.URI;
import java.util.List;

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
import won.protocol.model.ConnectionState;
import won.protocol.model.SocketType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.WonRdfUtils;

/**
 * User: Danijel Date: 26.3.14.
 */
public class BAAtomicCCCoordinatorSocketImpl extends AbstractBASocket {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ConnectionRepository connectionRepository;
    private BAStateManager stateManager;

    @Override
    public SocketType getSocketType() {
        return SocketType.BAAtomicCCCoordinatorSocket;
    }

    // Acceptance received from Participant
    public void openFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        // inform the atom side
        // CONNECTED state
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // adding Participants is possible only in the first phase
                    if (isCoordinatorInFirstPhase(con)) // add a new Participant (first phase in progress)
                    {
                        stateManager.setStateForAtomUri(BACCState.ACTIVE.getURI(),
                                        URI.create(WON_TX.PHASE_FIRST.getURI().toString()), con.getAtomURI(),
                                        con.getTargetAtomURI(), getSocketType().getURI());
                        storeBAStateForConnection(con, BACCState.ACTIVE.getURI());
                        // TODO: use new system
                        // ownerFacingConnectionClient.open(con.getConnectionURI(), content,
                        // wonMessage);
                        logger.debug("*** opened from Participant = coordinator:" + con.getAtomURI() + " participant:"
                                        + con.getTargetAtomURI() + " con:" + con.getConnectionURI() + " baState:"
                                        + stateManager.getStateForAtomUri(con.getAtomURI(), con.getTargetAtomURI(),
                                                        getSocketType().getURI()).toString()
                                        + "phase:"
                                        + BACCState.parsePhase(stateManager.getStatePhaseForAtomUri(con.getAtomURI(),
                                                        con.getTargetAtomURI(), getSocketType().getURI()).toString()));
                    } else // second phase in progress, new participants can not be added anymore
                    {
                        logger.debug("It is not possible to add more participants. The second phase of the protocol has already been started.");
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("", "no:uri");
                        Resource res = myContent.createResource("no:uri");
                        // TODO: add an explanation (error message) to the model, detailing that it's
                        // too late to
                        // participate in the transaction now
                        // close the initiated connection
                        // TODO: use new system
                        // atomFacingConnectionClient.close(con, myContent, wonMessage); //abort sent to
                        // participant
                    }
                } catch (Exception e) {
                    logger.warn("could not handle participant connect2", e);
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
                            if (isCoordinatorInFirstPhase(con) && eventType.getURI().toString()
                                            .equals(WON_TX.MESSAGE_CLOSE.getURI().toString())) {
                                // check whether all connections are in the state COMPLETED (all participant
                                // voted with YES)
                                // if yes -> send message and firstPhase is ended;
                                // if no -> wait for other participants to vote
                                if (canFirstPhaseFinish(con)) {
                                    logger.debug("First phase can be finished.");
                                    BACCState state = BACCState.parseString(stateManager
                                                    .getStateForAtomUri(con.getAtomURI(), con.getTargetAtomURI(),
                                                                    getSocketType().getURI())
                                                    .toString());
                                    logger.debug("Current state of the Coordinator {} for participant {}: ",
                                                    state.getURI().toString(), con.getTargetAtomURI().toString());
                                    logger.debug("This Coordinator state phase: {}", BACCState.parsePhase(stateManager
                                                    .getStateForAtomUri(con.getAtomURI(), con.getTargetAtomURI(),
                                                                    getSocketType().getURI())
                                                    .toString()));
                                    state = state.transit(eventType);
                                    state.setPhase(BACCState.Phase.SECOND); // Second phase is starting!
                                    stateManager.setStateForAtomUri(state.getURI(),
                                                    URI.create(WON_TX.PHASE_SECOND.getURI().toString()),
                                                    con.getAtomURI(), con.getTargetAtomURI(), getSocketType().getURI());
                                    storeBAStateForConnection(con, state.getURI());
                                    logger.debug("New state of the Coordinator:"
                                                    + stateManager.getStateForAtomUri(con.getAtomURI(),
                                                                    con.getTargetAtomURI(), getSocketType().getURI()));
                                    logger.debug("This Coordinator state phase: {}", BACCState.parsePhase(stateManager
                                                    .getStateForAtomUri(con.getAtomURI(), con.getTargetAtomURI(),
                                                                    getSocketType().getURI())
                                                    .toString()));
                                    // eventType -> URI Resource
                                    r = myContent.createResource(eventType.getURI().toString());
                                    baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                                    // TODO: use new system
                                    // atomFacingConnectionClient.sendMessage(con, myContent, wonMessage);
                                    // trigger the second phase in the corresponding states
                                    propagateSecondPhase(con);
                                } else {
                                    logger.debug("Not all votes are received from Participants!");
                                }
                            } else {
                                BACCState state = BACCState.parseString(stateManager
                                                .getStateForAtomUri(con.getAtomURI(), con.getTargetAtomURI(),
                                                                getSocketType().getURI())
                                                .toString());
                                BACCState newState = null;
                                logger.debug("Current state of the Coordinator {} for participant {}: ",
                                                state.getURI().toString(), con.getTargetAtomURI());
                                logger.debug("This Coordinator state phase: {}",
                                                BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                                con.getTargetAtomURI(), getSocketType().getURI())
                                                                .toString()));
                                newState = state.transit(eventType);
                                BACCState.Phase newPhase;
                                if (!(isCoordinatorInFirstPhase(con) && eventType.getURI().toString()
                                                .equals(WON_TX.MESSAGE_CANCEL.getURI().toString())))
                                    newPhase = BACCState.parsePhase(stateManager
                                                    .getStatePhaseForAtomUri(con.getAtomURI(), con.getTargetAtomURI(),
                                                                    getSocketType().getURI())
                                                    .toString());
                                else
                                    newPhase = BACCState.Phase.CANCELED_FROM_COORDINATOR; // propagate phase,
                                newState.setPhase(newPhase);
                                stateManager.setStateForAtomUri(newState.getURI(), BACCState.getPhaseURI(newPhase),
                                                con.getAtomURI(), con.getTargetAtomURI(), getSocketType().getURI());
                                storeBAStateForConnection(con, newState.getURI());
                                logger.debug("New state of the Coordinator:" + stateManager.getStateForAtomUri(
                                                con.getAtomURI(), con.getTargetAtomURI(), getSocketType().getURI()));
                                logger.debug("Coordinator phase: {}",
                                                BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                                con.getTargetAtomURI(), getSocketType().getURI())
                                                                .toString()));
                                // eventType -> URI Resource
                                r = myContent.createResource(eventType.getURI().toString());
                                baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                                // TODO: use new system
                                // atomFacingConnectionClient.sendMessage(con, myContent, wonMessage);
                            }
                        } else {
                            logger.debug("The eventType: " + eventType.getURI().toString()
                                            + " can not be triggered by Coordinator.");
                        }
                    } else {
                        logger.debug("The event type denoted by " + messageForSending + " is not allowed.");
                    }
                } catch (Exception e) {
                    logger.debug("caught Exception", e);
                }
            }
        });
    }

    public void setStateManager(final BAStateManager stateManager) {
        this.stateManager = stateManager;
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
                    BACCState state = BACCState.parseString(stateManager.getStateForAtomUri(con.getAtomURI(),
                                    con.getTargetAtomURI(), getSocketType().getURI()).toString());
                    logger.debug("Current state of the Coordinator {} for Participant {}. " + state.getURI().toString(),
                                    con.getTargetAtomURI());
                    // atomicBots outcome
                    // if one of the Participants votes NO (either EXIT or FAIL or CANNOTCOMPLETE is
                    // received)
                    // ACTITVE, COMPLETING -> (message: CANCEL) -> CANCELING
                    // COMPLETED -> (message: COMPENSATE) -> COMPENSATING
                    if (isCoordinatorInFirstPhase(con) && (eventType.getURI().toString()
                                    .equals(WON_TX.MESSAGE_EXIT.getURI().toString())
                                    || eventType.getURI().toString().equals(WON_TX.MESSAGE_FAIL.getURI().toString())
                                    || eventType.getURI().toString()
                                                    .equals(WON_TX.MESSAGE_CANNOTCOMPLETE.getURI().toString()))) {
                        state = state.transit(eventType);
                        state.setPhase(BACCState.Phase.SECOND); // The second phase is beginning
                        stateManager.setStateForAtomUri(state.getURI(),
                                        URI.create(WON_TX.PHASE_SECOND.getURI().toString()), con.getAtomURI(),
                                        con.getTargetAtomURI(), getSocketType().getURI());
                        storeBAStateForConnection(con, state.getURI());
                        logger.debug("New state of the Coordinator:" + stateManager.getStateForAtomUri(con.getAtomURI(),
                                        con.getTargetAtomURI(), getSocketType().getURI()));
                        logger.debug("This Coordinator state phase: {}",
                                        BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                        con.getTargetAtomURI(), getSocketType().getURI()).toString()));
                        logger.debug("Vote: NO ({})", eventType.getURI().toString());
                        // new Participants can not be added anymore
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("", "no:uri");
                        Resource baseResource = myContent.createResource("no:uri");
                        // list all Coordinator-Participant State Machines
                        List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
                        URI ownerUri = null;
                        URI atomUri = null;
                        for (Connection tmpCon : listOfCons) {
                            ownerUri = tmpCon.getAtomURI();
                            atomUri = tmpCon.getTargetAtomURI();
                            // process ACTIVE states and COMPLETING states
                            if (stateManager.getStateForAtomUri(ownerUri, atomUri, getSocketType().getURI()).toString()
                                            .equals(WON_TX.STATE_ACTIVE.getURI().toString())
                                            || stateManager.getStateForAtomUri(ownerUri, atomUri,
                                                            getSocketType().getURI()).toString()
                                                            .equals(WON_TX.STATE_COMPLETING.getURI().toString())) {
                                // Send MESSAGE_CANCEL to Participant
                                state = BACCState.parseString(stateManager
                                                .getStateForAtomUri(ownerUri, atomUri, getSocketType().getURI())
                                                .toString()); // must be
                                                              // Active
                                logger.debug("Current state of the Coordinator must be either ACTIVE or COMPLETING: "
                                                + state.getURI().toString());
                                logger.debug("Coordinator state phase: {}",
                                                BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                                con.getTargetAtomURI(), getSocketType().getURI())
                                                                .toString()));
                                eventType = BACCEventType
                                                .getBAEventTypeFromURI(WON_TX.MESSAGE_CANCEL.getURI().toString());
                                state = state.transit(eventType); // propagate the phase, it is better to have this
                                                                  // global
                                state.setPhase(BACCState.Phase.SECOND);
                                stateManager.setStateForAtomUri(state.getURI(),
                                                URI.create(WON_TX.PHASE_SECOND.getURI().toString()), ownerUri, atomUri,
                                                getSocketType().getURI());
                                // storeBAStateForConnection(con, state.getURI());
                                storeBAStateForConnection(tmpCon, state.getURI());
                                logger.debug("New state of the Coordinator must be CANCELING:" + stateManager
                                                .getStateForAtomUri(ownerUri, atomUri, getSocketType().getURI()));
                                logger.debug("Coordinator state phase must be second: {}",
                                                BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                                con.getTargetAtomURI(), getSocketType().getURI())
                                                                .toString()));
                                Resource r = myContent.createResource(BACCEventType.MESSAGE_CANCEL.getURI().toString());
                                baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                                // TODO: use new system
                                // atomFacingConnectionClient.sendMessage(tmpCon, myContent, wonMessage);
                                baseResource.removeAll(WON_TX.COORDINATION_MESSAGE);
                            }
                            // process COMPLETED state
                            else if (stateManager.getStateForAtomUri(ownerUri, atomUri, getSocketType().getURI())
                                            .toString().equals(WON_TX.STATE_COMPLETED.getURI().toString())) {
                                // Send MESSAGE_COMPENSATE to Participant
                                state = BACCState.parseString(stateManager
                                                .getStateForAtomUri(ownerUri, atomUri, getSocketType().getURI())
                                                .toString()); // must be
                                                              // Active
                                logger.debug("Current state of the Coordinator must be COMPLETED. The state is: {}",
                                                state.getURI().toString());
                                logger.debug("Coordinator state phase: {}",
                                                BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                                con.getTargetAtomURI(), getSocketType().getURI())
                                                                .toString()));
                                eventType = BACCEventType
                                                .getBAEventTypeFromURI(WON_TX.MESSAGE_COMPENSATE.getURI().toString());
                                state = state.transit(eventType); // propagate the phase, it is better to have this
                                                                  // global
                                state.setPhase(BACCState.Phase.SECOND);
                                stateManager.setStateForAtomUri(state.getURI(),
                                                URI.create(WON_TX.PHASE_SECOND.getURI().toString()), ownerUri, atomUri,
                                                getSocketType().getURI());
                                // storeBAStateForConnection(con, state.getURI());
                                storeBAStateForConnection(tmpCon, state.getURI());
                                logger.debug("New state of the Coordinator must be COMPENSATING. The state is: {}",
                                                stateManager.getStateForAtomUri(ownerUri, atomUri,
                                                                getSocketType().getURI()));
                                logger.debug("Coordinator state phase: {}",
                                                BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                                con.getTargetAtomURI(), getSocketType().getURI())
                                                                .toString()));
                                Resource r = myContent
                                                .createResource(BACCEventType.MESSAGE_COMPENSATE.getURI().toString());
                                baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                                // TODO: use new system
                                // atomFacingConnectionClient.sendMessage(tmpCon, myContent, wonMessage);
                                baseResource.removeAll(WON_TX.COORDINATION_MESSAGE);
                            }
                        }
                        // now, after sending messages to all partners, tell the owner
                        // TODO: use new system
                        // ownerFacingConnectionClient.sendMessage(con.getConnectionURI(), message,
                        // wonMessage);
                    } else {
                        BACCState newState = state.transit(eventType); // propagate the phase, it is better to have this
                                                                       // global
                        BACCState.Phase phase = BACCState
                                        .parsePhase(stateManager.getStatePhaseForAtomUri(con.getAtomURI(),
                                                        con.getTargetAtomURI(), getSocketType().getURI()).toString());
                        newState.setPhase(phase);
                        URI phaseURI = stateManager.getStatePhaseForAtomUri(con.getAtomURI(), con.getTargetAtomURI(),
                                        getSocketType().getURI());
                        stateManager.setStateForAtomUri(newState.getURI(), phaseURI, con.getAtomURI(),
                                        con.getTargetAtomURI(), getSocketType().getURI());
                        storeBAStateForConnection(con, newState.getURI());
                        logger.debug("New state of the Coordinator:" + stateManager.getStateForAtomUri(con.getAtomURI(),
                                        con.getTargetAtomURI(), getSocketType().getURI()));
                        logger.debug("Coordinator state phase: {}",
                                        BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                        con.getTargetAtomURI(), getSocketType().getURI()).toString()));
                        // TODO: use new system
                        // ownerFacingConnectionClient.sendMessage(con.getConnectionURI(), message,
                        // wonMessage);
                    }
                    BACCEventType resendEventType = state.getResendEvent();
                    if (resendEventType != null) {
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("", "no:uri");
                        Resource baseResource = myContent.createResource("no:uri");
                        if (BACCEventType.isBACCCoordinatorEventType(resendEventType)) {
                            state = BACCState.parseString(stateManager.getStateForAtomUri(con.getAtomURI(),
                                            con.getTargetAtomURI(), getSocketType().getURI()).toString());
                            BACCState newState = null;
                            logger.debug("Coordinator re-sends the previous message.");
                            logger.debug("Current state of the Coordinator: " + state.getURI().toString());
                            newState = state.transit(resendEventType); // propagate the phase, it is better to have this
                                                                       // global
                            // newState.setPhase(state.getPhase());
                            URI phaseURI = stateManager.getStatePhaseForAtomUri(con.getAtomURI(),
                                            con.getTargetAtomURI(), getSocketType().getURI());
                            stateManager.setStateForAtomUri(newState.getURI(), phaseURI, con.getAtomURI(),
                                            con.getTargetAtomURI(), getSocketType().getURI());
                            storeBAStateForConnection(con, newState.getURI());
                            logger.debug("New state of the Coordinator:" + stateManager.getStateForAtomUri(
                                            con.getAtomURI(), con.getTargetAtomURI(), getSocketType().getURI()));
                            logger.debug("Coordinator state phase: {}",
                                            BACCState.parsePhase(stateManager.getStateForAtomUri(con.getAtomURI(),
                                                            con.getTargetAtomURI(), getSocketType().getURI())
                                                            .toString()));
                            // eventType -> URI Resource
                            Resource r = myContent.createResource(resendEventType.getURI().toString());
                            baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                            // TODO: use new system
                            // atomFacingConnectionClient.sendMessage(con, myContent, wonMessage);
                        } else {
                            logger.debug("The eventType: " + eventType.getURI().toString() + " can not be triggered by "
                                            + "Participant.");
                        }
                    }
                } catch (Exception e) {
                    logger.debug("caught Exception", e);
                }
            }
        });
    }

    private boolean canFirstPhaseFinish(Connection con) {
        List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
        URI ownerUri = null;
        URI atomUri = null;
        URI currentStateUri = null;
        for (Connection tmpCon : listOfCons) {
            ownerUri = tmpCon.getAtomURI();
            atomUri = tmpCon.getTargetAtomURI();
            currentStateUri = BACCState.parseString(
                            stateManager.getStateForAtomUri(ownerUri, atomUri, getSocketType().getURI()).toString())
                            .getURI();
            if (currentStateUri.toString().equals(WON_TX.STATE_ACTIVE.getURI().toString())
                            || currentStateUri.toString().equals(WON_TX.STATE_COMPLETING.getURI().toString()))
                return false;
        }
        return true;
    }

    private List<Connection> getAllCoordinatorParticipantConnections(Connection con) {
        List<Connection> listOfCons = connectionRepository.findByAtomURIAndStateAndTypeURI(con.getAtomURI(),
                        ConnectionState.CONNECTED, SocketType.BAAtomicCCCoordinatorSocket.getURI());
        return listOfCons;
    }

    private boolean isCoordinatorInFirstPhase(Connection con) {
        List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
        URI ownerUri, atomUri, currentStateURI;
        BACCState currentState;
        logger.debug("Checking if coordinator is in the first phase for coordinator " + con.getAtomURI() + "participant"
                        + con.getTargetAtomURI() + "and connection" + con.getConnectionURI());
        for (Connection c : listOfCons) {
            ownerUri = c.getAtomURI();
            atomUri = c.getTargetAtomURI();
            logger.debug("checking if coordinator is in first phase for coordinator {} and participant {}", ownerUri,
                            atomUri);
            currentStateURI = stateManager.getStateForAtomUri(ownerUri, atomUri, getSocketType().getURI());
            logger.debug("Current coordinator state URI is {}: ", currentStateURI);
            if (currentStateURI == null) {
                // this happens when a connection is being opened in a parallel thread
                // and execution hasn't yet reached the point where its state is recorded
                // That other connection definitely hasn't caused phase 2 to start, so
                // we can safely say we're still in phase 1.
                // Therefore, continue with the next check.
                logger.debug("ignoring connection {} for phase one check as it"
                                + " hasn't been processed by the socket logic yet", c.getConnectionURI());
                continue;
            }
            BACCState.Phase phase = BACCState.parsePhase(stateManager
                            .getStatePhaseForAtomUri(ownerUri, atomUri, getSocketType().getURI()).toString());
            if (phase == null) // redundant, this or the previous condition can be omitted
            {
                continue;
            }
            if (BACCState.getPhaseURI(phase).toString().equals(WON_TX.PHASE_SECOND.getURI().toString())) {
                return false;
            }
        }
        return true;
    }

    private void propagateSecondPhase(Connection con) {
        List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
        BACCState state;
        for (Connection currentCon : listOfCons) {
            state = BACCState.parseString(stateManager.getStateForAtomUri(currentCon.getAtomURI(),
                            currentCon.getTargetAtomURI(), getSocketType().getURI()).toString());
            if (state.getPhase() == BACCState.Phase.FIRST) {
                state.setPhase(BACCState.Phase.SECOND);
                stateManager.setStateForAtomUri(state.getURI(), URI.create(WON_TX.PHASE_SECOND.getURI().toString()),
                                currentCon.getAtomURI(), currentCon.getTargetAtomURI(), getSocketType().getURI());
                storeBAStateForConnection(currentCon, state.getURI());
            }
        }
    }
}