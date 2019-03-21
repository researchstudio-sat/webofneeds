package won.node.facet.impl;

import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.businessactivity.participantcompletion.BAPCEventType;
import won.node.facet.businessactivity.participantcompletion.BAPCState;
import won.node.facet.businessactivity.statemanager.BAStateManager;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 * User: Danijel Date: 19.3.14.
 */
public class BAAtomicPCCoordinatorFacetImpl extends AbstractBAFacet {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private BAStateManager stateManager;

  @Override
  public FacetType getFacetType() {
    return FacetType.BAAtomicPCCoordinatorFacet;
  }

  // Acceptance received from Participant
  public void openFromNeed(final Connection con, final Model content, final WonMessage wonMessage)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    // inform the need side
    // CONNECTED state
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          // adding Participants is possible only in the first phase
          if (isCoordinatorInFirstPhase(con)) // add a new Participant (first phase in progress)
          {
            logger.debug("Open from Participant (coordinator:" + con.getNeedURI() + " participant:"
                + con.getRemoteNeedURI() + " con:" + con.getConnectionURI());

            stateManager.setStateForNeedUri(BAPCState.ACTIVE.getURI(),
                URI.create(WON_TX.PHASE_FIRST.getURI().toString()), con.getNeedURI(), con.getRemoteNeedURI(),
                getFacetType().getURI());
            storeBAStateForConnection(con, BAPCState.ACTIVE.getURI());
            // TODO: use new system
            // ownerFacingConnectionClient.open(con.getConnectionURI(), content, null);

            logger.debug("Opened from Participant = coordinator:" + con.getNeedURI() + " participant:"
                + con.getRemoteNeedURI() + " con:" + con.getConnectionURI() + " baState:"
                + stateManager
                    .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString()
                + "phase:"
                + BAPCState.parsePhase(stateManager
                    .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                    .toString()));
          } else // second phase in progress, new participants can not be added anymore
          {
            logger.debug(
                "It is not possible to add more participants. The second phase of the protocol has already been started.");
            Model myContent = ModelFactory.createDefaultModel();
            myContent.setNsPrefix("", "no:uri");
            Resource res = myContent.createResource("no:uri");
            // close the initiated connection
            // TODO: use new system
            // needFacingConnectionClient.close(con, myContent, wonMessage);
          }
        } catch (Exception e) {
          logger.debug("could not handle participant connect", e);
        }
      }

    });
  }

  // Coordinator sends message to Participant
  public void sendMessageFromOwner(final Connection con, final Model message, final WonMessage wonMessage)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    final URI remoteConnectionURI = con.getRemoteConnectionURI();
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

            logger.debug("Coordinator sends the text message:" + eventType + " coordinator:" + con.getNeedURI()
                + " participant:" + con.getRemoteNeedURI() + " con:" + con.getConnectionURI() + " baState:"
                + stateManager
                    .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString()
                + " phase:"
                + BAPCState.parsePhase(stateManager
                    .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                    .toString()));
          }

          // message as MODEL
          else {
            NodeIterator ni = message
                .listObjectsOfProperty(message.getProperty(WON_TX.COORDINATION_MESSAGE.getURI().toString()));
            if (ni.hasNext()) {
              String eventTypeURI = ni.toList().get(0).asResource().getURI().toString();
              eventType = BAPCEventType.getBAEventTypeFromURI(eventTypeURI);

              logger.debug("Coordinator sends the RDF message:" + eventType.getURI() + " coordinator:"
                  + con.getNeedURI() + " participant:" + con.getRemoteNeedURI() + " con:" + con.getConnectionURI()
                  + " baState:"
                  + stateManager
                      .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString()
                  + " phase:"
                  + BAPCState.parsePhase(stateManager
                      .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                      .toString()));
            } else {
              logger.debug("Message {} does not contain a proper content.", message.toString());
              return;
            }
          }
          myContent = ModelFactory.createDefaultModel();
          myContent.setNsPrefix("", "no:uri");
          Resource baseResource = myContent.createResource("no:uri");

          if ((eventType != null)) {
            if (eventType.isBAPCCoordinatorEventType(eventType)) {
              if (isCoordinatorInFirstPhase(con)
                  && eventType.getURI().toString().equals(WON_TX.MESSAGE_CLOSE.getURI().toString())) {
                // check whether all connections are in the state COMPLETED (all participant
                // voted with YES)
                // if yes -> send message and firstPhase is ended;
                // if no -> wait for other participants to vote
                if (canFirstPhaseFinish(con)) {
                  BAPCState state = BAPCState.parseString(
                      stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                          .toString());

                  state = state.transit(eventType);
                  state.setPhase(BAPCState.Phase.SECOND); // Second phase is starting!

                  stateManager.setStateForNeedUri(state.getURI(), URI.create(WON_TX.PHASE_SECOND.getURI().toString()),
                      con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI());
                  storeBAStateForConnection(con, state.getURI());

                  logger.debug("New state = coordinator:" + con.getNeedURI() + " participant:" + con.getRemoteNeedURI()
                      + " con:" + con.getConnectionURI() + " baState:"
                      + stateManager
                          .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                          .toString()
                      + " phase:"
                      + BAPCState.parsePhase(stateManager
                          .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                          .toString()));

                  // eventType -> URI Resource
                  r = myContent.createResource(eventType.getURI().toString());
                  baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                  // TODO: use new system
                  // needFacingConnectionClient.sendMessage(con, myContent, wonMessage);

                  logger.debug("Coordinator sent message to participant");

                  // trigger the second phase in the corresponding states
                  propagateSecondPhase(con);
                } else {
                  logger.debug("Not all votes are received from Participants! First phase can not finish.");
                }
              } else {
                BAPCState state = BAPCState.parseString(stateManager
                    .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());
                BAPCState newState = null;
                newState = state.transit(eventType);

                BAPCState.Phase newPhase;

                if (!(isCoordinatorInFirstPhase(con)
                    && eventType.getURI().toString().equals(WON_TX.MESSAGE_CANCEL.getURI().toString())))
                  newPhase = BAPCState.parsePhase(stateManager
                      .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                      .toString());
                else
                  newPhase = BAPCState.Phase.CANCELED_FROM_COORDINATOR; // propagate phase,
                newState.setPhase(newPhase);

                stateManager.setStateForNeedUri(newState.getURI(), BAPCState.getPhaseURI(newPhase), con.getNeedURI(),
                    con.getRemoteNeedURI(), getFacetType().getURI());
                storeBAStateForConnection(con, newState.getURI());

                logger.debug("New state = coordinator:" + con.getNeedURI() + " participant:" + con.getRemoteNeedURI()
                    + " con:" + con.getConnectionURI() + " baState:"
                    + stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                        .toString()
                    + " phase:"
                    + BAPCState.parsePhase(stateManager
                        .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                        .toString()));

                // eventType -> URI Resource
                r = myContent.createResource(eventType.getURI().toString());
                baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                // TODO: use new system
                // needFacingConnectionClient.sendMessage(con, myContent, wonMessage);
                logger.debug("Coordinator sent message to participant");
              }
            } else {
              logger.debug("The eventType: {} can not be triggered by Coordinator.", eventType.getURI().toString());
            }
          } else {
            logger.debug("The event type denoted by {} is not allowed.", messageForSending);
          }
        } catch (Exception e) {
          logger.debug("caught Exception", e);
        }
      }
    });
  }

  // Coordinator receives message from Participant
  public void sendMessageFromNeed(final Connection con, final Model message, final WonMessage wonMessage)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    // send to the need side
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          logger.debug("Coordinator receives message:+" + message.toString() + "= coordinator:" + con.getNeedURI()
              + " participant:" + con.getRemoteNeedURI() + " con:" + con.getConnectionURI() + " baState:"
              + stateManager
                  .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString()
              + " phase:"
              + BAPCState.parsePhase(stateManager
                  .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                  .toString()));

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

          BAPCState state = BAPCState.parseString(stateManager
              .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());

          // atomicBots outcome
          // if one of the Participants votes NO (either EXIT or FAIL or CANNOTCOMPLETE is
          // received)
          // ACTITVE -> (message: CANCEL) -> CANCELING
          // COMPLETED -> (message: COMPENSATE) -> COMPENSATING
          boolean isCFP = isCoordinatorInFirstPhase(con);

          if (isCFP && (eventType.getURI().toString().equals(WON_TX.MESSAGE_EXIT.getURI().toString())
              || eventType.getURI().toString().equals(WON_TX.MESSAGE_FAIL.getURI().toString())
              || eventType.getURI().toString().equals(WON_TX.MESSAGE_CANNOTCOMPLETE.getURI().toString()))) {
            logger.debug("Coordinator receives NO vote. Is it in the first phase? " + isCFP + " "
                + eventType.getURI().toString() + con.getNeedURI() + " " + con.getRemoteNeedURI());

            state = state.transit(eventType);
            state.setPhase(BAPCState.Phase.SECOND); // The second phase is beginning
            stateManager.setStateForNeedUri(state.getURI(), URI.create(WON_TX.PHASE_SECOND.getURI().toString()),
                con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI());
            storeBAStateForConnection(con, state.getURI());

            logger.debug("New state = coordinator:" + con.getNeedURI() + " participant:" + con.getRemoteNeedURI()
                + " con:" + con.getConnectionURI() + " baState:"
                + stateManager
                    .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString()
                + " phase must be SECOND:"
                + BAPCState.parsePhase(stateManager
                    .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                    .toString()));

            logger.debug("NO received");

            // new Participants can not be added anymore
            Model myContent = ModelFactory.createDefaultModel();
            myContent.setNsPrefix("", "no:uri");
            Resource baseResource = myContent.createResource("no:uri");

            // list all Coordinator-Participant State Machines
            List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
            URI ownerUri = null;
            URI needUri = null;

            logger.debug("Coordinator sends global NO");

            for (Connection tmpCon : listOfCons) {
              ownerUri = tmpCon.getNeedURI();
              needUri = tmpCon.getRemoteNeedURI();

              // process ACTIVE states
              if (stateManager.getStateForNeedUri(ownerUri, needUri, getFacetType().getURI()).toString()
                  .equals(WON_TX.STATE_ACTIVE.getURI().toString())) {
                // Send MESSAGE_CANCEL to Participant
                state = BAPCState.parseString(
                    stateManager.getStateForNeedUri(ownerUri, needUri, getFacetType().getURI()).toString()); // must be
                                                                                                             // Active

                logger
                    .debug(
                        "GLOBAL NO, actions for ACTIVE states = coordinator:" + tmpCon.getNeedURI() + " participant:"
                            + tmpCon.getRemoteNeedURI() + " tmpCon:" + tmpCon.getConnectionURI() + " baState:"
                            + stateManager.getStateForNeedUri(tmpCon.getNeedURI(), tmpCon.getRemoteNeedURI(),
                                getFacetType().getURI()).toString()
                            + " phase must be FIRST:"
                            + BAPCState.parsePhase(stateManager.getStatePhaseForNeedUri(tmpCon.getNeedURI(),
                                tmpCon.getRemoteNeedURI(), getFacetType().getURI()).toString()));

                eventType = BAPCEventType.getBAEventTypeFromURI(WON_TX.MESSAGE_CANCEL.getURI().toString());

                state = state.transit(eventType); // propagate the phase, it is better to have this global
                state.setPhase(BAPCState.Phase.SECOND);
                stateManager.setStateForNeedUri(state.getURI(), URI.create(WON_TX.PHASE_SECOND.getURI().toString()),
                    ownerUri, needUri, getFacetType().getURI());
                storeBAStateForConnection(tmpCon, state.getURI());

                logger
                    .debug(
                        "GLOBAL NO, changes (CANCELING) = coordinator:" + tmpCon.getNeedURI() + " participant:"
                            + tmpCon.getRemoteNeedURI() + " tmpCon:" + tmpCon.getConnectionURI() + " baState:"
                            + stateManager.getStateForNeedUri(tmpCon.getNeedURI(), tmpCon.getRemoteNeedURI(),
                                getFacetType().getURI()).toString()
                            + " phase must be SECOND:"
                            + BAPCState.parsePhase(stateManager.getStatePhaseForNeedUri(tmpCon.getNeedURI(),
                                tmpCon.getRemoteNeedURI(), getFacetType().getURI()).toString()));

                Resource r = myContent.createResource(BAPCEventType.MESSAGE_CANCEL.getURI().toString());
                baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                // TODO: use new system
                // needFacingConnectionClient.sendMessage(tmpCon, myContent, wonMessage);

                baseResource.removeAll(WON_TX.COORDINATION_MESSAGE);
              }
              // process COMPLETED state
              else if (stateManager.getStateForNeedUri(ownerUri, needUri, getFacetType().getURI()).toString()
                  .equals(WON_TX.STATE_COMPLETED.getURI().toString())) {

                // Send MESSAGE_COMPENSATE to Participant
                state = BAPCState.parseString(
                    stateManager.getStateForNeedUri(ownerUri, needUri, getFacetType().getURI()).toString()); // must be
                                                                                                             // COMPLETED

                logger
                    .debug(
                        "GLOBAL NO, COMPLETED actions = coordinator:" + tmpCon.getNeedURI() + " participant:"
                            + tmpCon.getRemoteNeedURI() + " tmpCon:" + tmpCon.getConnectionURI() + " baState:"
                            + stateManager.getStateForNeedUri(tmpCon.getNeedURI(), tmpCon.getRemoteNeedURI(),
                                getFacetType().getURI()).toString()
                            + " phase must be FIRST:"
                            + BAPCState.parsePhase(stateManager.getStatePhaseForNeedUri(tmpCon.getNeedURI(),
                                tmpCon.getRemoteNeedURI(), getFacetType().getURI()).toString()));

                eventType = BAPCEventType.getBAEventTypeFromURI(WON_TX.MESSAGE_COMPENSATE.getURI().toString());

                state = state.transit(eventType); // propagate the phase, it is better to have this global
                state.setPhase(BAPCState.Phase.SECOND);
                stateManager.setStateForNeedUri(state.getURI(), URI.create(WON_TX.PHASE_SECOND.getURI().toString()),
                    ownerUri, needUri, getFacetType().getURI());
                storeBAStateForConnection(tmpCon, state.getURI());

                logger
                    .debug(
                        "GLOBAL NO, changes (COMPENSATE) = coordinator:" + tmpCon.getNeedURI() + " participant:"
                            + tmpCon.getRemoteNeedURI() + " tmpCon:" + tmpCon.getConnectionURI() + " baState:"
                            + stateManager.getStateForNeedUri(tmpCon.getNeedURI(), tmpCon.getRemoteNeedURI(),
                                getFacetType().getURI()).toString()
                            + " phase must be SECOND:"
                            + BAPCState.parsePhase(stateManager.getStatePhaseForNeedUri(tmpCon.getNeedURI(),
                                tmpCon.getRemoteNeedURI(), getFacetType().getURI()).toString()));

                Resource r = myContent.createResource(BAPCEventType.MESSAGE_COMPENSATE.getURI().toString());
                baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                // TODO: use new system
                // needFacingConnectionClient.sendMessage(tmpCon, myContent, wonMessage);
                baseResource.removeAll(WON_TX.COORDINATION_MESSAGE);
              }
            }
            // now, after all messages were sent to partners, tell the owner.
            // TODO: use new system
            // ownerFacingConnectionClient.sendMessage(con.getConnectionURI(), message,
            // null);
          } else {
            BAPCState newState = state.transit(eventType); // propagate the phase, it is better to have this global
            BAPCState.Phase phase = BAPCState.parsePhase(stateManager
                .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());
            newState.setPhase(phase);

            URI phaseURI = stateManager.getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(),
                getFacetType().getURI());
            stateManager.setStateForNeedUri(newState.getURI(), phaseURI, con.getNeedURI(), con.getRemoteNeedURI(),
                getFacetType().getURI());
            storeBAStateForConnection(con, newState.getURI());

            logger.debug("New state = coordinator:" + con.getNeedURI() + " participant:" + con.getRemoteNeedURI()
                + " con:" + con.getConnectionURI() + " baState:"
                + stateManager
                    .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString()
                + " phase:"
                + BAPCState.parsePhase(stateManager
                    .getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI())
                    .toString()));

            // TODO: use new system
            // ownerFacingConnectionClient.sendMessage(con.getConnectionURI(), message,
            // null);

            logger.debug("Coordinator received the message.");
          }

          BAPCEventType resendEventType = state.getResendEvent();
          if (resendEventType != null) {
            logger.debug("RESENDING!!!!");
            Model myContent = ModelFactory.createDefaultModel();
            myContent.setNsPrefix("", "no:uri");
            Resource baseResource = myContent.createResource("no:uri");

            if (BAPCEventType.isBAPCCoordinatorEventType(resendEventType)) {
              state = BAPCState.parseString(stateManager
                  .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());
              BAPCState newState = null;
              logger.debug("Coordinator re-sends the previous message.");
              logger.debug("Current state of the Coordinator: " + state.getURI().toString());

              newState = state.transit(resendEventType); // propagate the phase, it is better to have this global

              URI phaseURI = stateManager.getStatePhaseForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(),
                  getFacetType().getURI());
              stateManager.setStateForNeedUri(newState.getURI(), phaseURI, con.getNeedURI(), con.getRemoteNeedURI(),
                  getFacetType().getURI());
              storeBAStateForConnection(con, newState.getURI());

              logger.debug("New state of the Coordinator:"
                  + stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()));
              logger.debug("Coordinator state indicator: {}",
                  BAPCState.parseString(stateManager
                      .getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString())
                      .getPhase());

              // eventType -> URI Resource
              Resource r = myContent.createResource(resendEventType.getURI().toString());
              baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
              // TODO: use new system
              // needFacingConnectionClient.sendMessage(con, myContent, wonMessage);
            } else {
              logger.debug(
                  "The eventType: " + eventType.getURI().toString() + " can not be triggered by " + "Participant.");
            }
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

  private boolean canFirstPhaseFinish(Connection con) {
    List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);

    URI ownerUri = null;
    URI needUri = null;
    URI currentStateUri = null;
    for (Connection tmpCon : listOfCons) {
      ownerUri = tmpCon.getNeedURI();
      needUri = tmpCon.getRemoteNeedURI();
      currentStateUri = stateManager.getStateForNeedUri(ownerUri, needUri, getFacetType().getURI());
      if (currentStateUri.toString().equals(WON_TX.STATE_ACTIVE.getURI().toString()))
        return false;
    }
    return true;
  }

  private List<Connection> getAllCoordinatorParticipantConnections(Connection con) {
    List<Connection> listOfCons = connectionRepository.findByNeedURIAndStateAndTypeURI(con.getNeedURI(),
        ConnectionState.CONNECTED, FacetType.BAAtomicPCCoordinatorFacet.getURI());
    return listOfCons;
  }

  private boolean isCoordinatorInFirstPhase(Connection con) {
    List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
    URI ownerUri, needUri, currentStateURI;
    BAPCState currentState;
    for (Connection c : listOfCons) {
      ownerUri = c.getNeedURI();
      needUri = c.getRemoteNeedURI();
      currentStateURI = stateManager.getStateForNeedUri(ownerUri, needUri, getFacetType().getURI());
      // logger.debug("Current state URI is {}: ", currentStateURI);
      if (currentStateURI == null) {
        // this happens when a connection is being opened in a parallel thread
        // and execution hasn't yet reached the point where its state is recorded
        // That other connection definitely hasn't caused phase 2 to start, so
        // we can safely say we're still in phase 1.
        // Therefore, continue with the next check.
        // logger.debug("ignoring connection {} for phase one check as it" +
        // " hasn't been processed by the facet logic yet",
        // c.getConnectionURI());
        continue;
      }
      currentState = BAPCState.parseString(currentStateURI.toString());
      // logger.debug("phase is {}: ", currentState.getPhase());
      BAPCState.Phase phase = BAPCState
          .parsePhase(stateManager.getStatePhaseForNeedUri(ownerUri, needUri, getFacetType().getURI()).toString());
      if (phase == null) // redundant, this or the previous condition can be omitted
      {
        continue;
      }
      if (BAPCState.getPhaseURI(phase).toString().equals(WON_TX.PHASE_SECOND.getURI().toString())) {
        return false;
      }
    }
    return true;
  }

  private void propagateSecondPhase(Connection con) {
    List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
    BAPCState state;
    for (Connection currentCon : listOfCons) {
      state = BAPCState.parseString(stateManager
          .getStateForNeedUri(currentCon.getNeedURI(), currentCon.getRemoteNeedURI(), getFacetType().getURI())
          .toString());
      BAPCState.Phase phase = BAPCState.parsePhase(stateManager
          .getStatePhaseForNeedUri(currentCon.getNeedURI(), currentCon.getRemoteNeedURI(), getFacetType().getURI())
          .toString());
      if (BAPCState.getPhaseURI(phase).toString().equals(WON_TX.PHASE_FIRST.getURI().toString())) {
        state.setPhase(BAPCState.Phase.SECOND);
        stateManager.setStateForNeedUri(state.getURI(), URI.create(WON_TX.PHASE_SECOND.getURI().toString()),
            currentCon.getNeedURI(), currentCon.getRemoteNeedURI(), getFacetType().getURI());
        storeBAStateForConnection(currentCon, state.getURI());
      }
    }
  }
}