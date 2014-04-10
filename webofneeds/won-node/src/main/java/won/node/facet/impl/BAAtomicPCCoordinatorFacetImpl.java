package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.businessactivity.participantcompletion.BAPCEventType;
import won.node.facet.businessactivity.participantcompletion.BAPCState;
import won.node.facet.businessactivity.participantcompletion.SimpleBAPCStateManager;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;

import java.net.URI;
import java.util.List;

/**
 * User: Danijel
 * Date: 19.3.14.
 */
public class BAAtomicPCCoordinatorFacetImpl extends AbstractFacet{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private SimpleBAPCStateManager stateManager = new SimpleBAPCStateManager();

  @Autowired
  private ConnectionRepository connectionRepository;

  @Override
  public FacetType getFacetType() {
      return FacetType.BAAtomicPCCoordinatorFacet;
  }

  //Acceptance received from Participant
  public void openFromNeed(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the need side
    //CONNECTED state
    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try {
          ownerFacingConnectionClient.open(con.getConnectionURI(), content);
          stateManager.setStateForNeedUri(BAPCState.ACTIVE, con.getNeedURI(), con.getRemoteNeedURI());
          logger.debug("Coordinator state: "+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
          logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());
        } catch (WonProtocolException e) {
          logger.debug("caught Exception:", e);
        }

        // adding Participants is possible only in the first phase
        if(isCoordinatorInFirstPhase(con))  //add a new Participant (first phase in progress)
        {
          stateManager.setStateForNeedUri(BAPCState.ACTIVE, con.getNeedURI(), con.getRemoteNeedURI());
          logger.debug("Coordinator state: " + stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
          logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());
        }
        else  // second phase in progress, new participants can not be added anymore
        {
          logger.debug("It is not possible to add more participants. The second phase of the protocol has already been started.");
          Model myContent = ModelFactory.createDefaultModel();
          myContent.setNsPrefix("","no:uri");
          Resource res = myContent.createResource("no:uri");
          //close the initiated connection
          try {
            ownerFacingConnectionClient.close(con.getConnectionURI(), myContent);
            needFacingConnectionClient.close(con, myContent);   //abort sent to participant
          } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
          }
        }
      }
    });
  }

  //Coordinator sends message to Participant
  public void textMessageFromOwner(final Connection con, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    final URI remoteConnectionURI = con.getRemoteConnectionURI();
    //inform the other side
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          String messageForSending = new String();
          BAPCEventType eventType = null;
          Model myContent = null;
          Resource r = null;

          //message (event) for sending

          //message as TEXT
          NodeIterator ni = message.listObjectsOfProperty(message.getProperty(WON_BA.BASE_URI,"hasTextMessage"));
          if (ni.hasNext())
          {
            messageForSending = ni.toList().get(0).toString();
            messageForSending = messageForSending.substring(0, messageForSending.indexOf("^^http:"));
            eventType = BAPCEventType.getCoordinationEventTypeFromString(messageForSending);
            logger.debug("Coordinator sends the text message: {}", eventType.getURI());
          }

          //message as MODEL
          else {
            ni = message.listObjectsOfProperty(message.getProperty(WON_BA.COORDINATION_MESSAGE.getURI().toString()));
            if(ni.hasNext())
            {
              String eventTypeURI = ni.toList().get(0).asResource().getURI().toString();
              eventType = BAPCEventType.getBAEventTypeFromURI(eventTypeURI);
              logger.debug("Coordinator sends the RDF: {}", eventType.getURI());
            }
            else
            {
              logger.debug("Message {} does not contain a proper content.", message.toString());
              return;
            }
          }
          myContent = ModelFactory.createDefaultModel();
          myContent.setNsPrefix("","no:uri");
          Resource baseResource = myContent.createResource("no:uri");

          if((eventType!=null))
          {
            if(eventType.isBAPCCoordinatorEventType(eventType))
            {
              if(isCoordinatorInFirstPhase(con) && eventType.getURI().toString().equals(WON_BA.MESSAGE_CLOSE.getURI().toString()))
              {
                //check whether all connections are in the state COMPLETED (all participant voted with YES)
                // if yes -> send message and firstPhase is ended;
                // if no -> wait for other participants to vote
                if(canFirstPhaseFinish(con))
                {
                  BAPCState state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                  logger.debug("Current state of the Coordinator: "+state.getURI().toString());
                  logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

                  state = state.transit(eventType);
                  state.setPhase(BAPCState.Phase.SECOND); //Second phase is starting!
                  stateManager.setStateForNeedUri(state, con.getNeedURI(), con.getRemoteNeedURI());
                  logger.debug("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
                  logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

                  // eventType -> URI Resource
                  r = myContent.createResource(eventType.getURI().toString());
                  baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                  needFacingConnectionClient.textMessage(con, myContent);

                  //trigger the second phase in the corresponding states
                  propagateSecondPhase(con);
                }
                else
                {
                  logger.debug("Not all votes are received from Participants!");
                }
              }
              else
              {
                BAPCState state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                BAPCState newState = null;
                logger.debug("Current state of the Coordinator: "+state.getURI().toString());
                logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

                newState = state.transit(eventType);

                if(!(isCoordinatorInFirstPhase(con) && eventType.getURI().toString().equals(WON_BA.MESSAGE_CANCEL.getURI().toString())))
                  newState.setPhase(state.getPhase());
                else
                  newState.setPhase(BAPCState.Phase.CANCELED_FROM_COORDINATOR); //propagate phase,

                stateManager.setStateForNeedUri(newState, con.getNeedURI(), con.getRemoteNeedURI());
                logger.debug("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
                logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

                // eventType -> URI Resource
                r = myContent.createResource(eventType.getURI().toString());
                baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                needFacingConnectionClient.textMessage(con, myContent);
              }
            }
            else
            {
              logger.debug("The eventType: "+eventType.getURI().toString()+" can not be triggered by Coordinator.");
            }
          }
          else
          {
            logger.debug("The event type denoted by "+messageForSending+" is not allowed.");
          }
        } catch (WonProtocolException e) {
          logger.warn("caught WonProtocolException:", e);
        } catch (Exception e) {
          logger.debug("caught Exception", e);
        }
      }
    });
  }

  //Coordinator receives message from Participant
  public void textMessageFromNeed(final Connection con, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //send to the need side
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          logger.debug("Received message from Participant: " + message.toString());
          NodeIterator it = message.listObjectsOfProperty(WON_BA.COORDINATION_MESSAGE);
          if (!it.hasNext()) {
            logger.debug("message did not contain a won-ba:coordinationMessage");
            return;
          }
          RDFNode coordMsgNode = it.nextNode();
          if (!coordMsgNode.isURIResource()){
            logger.debug("message did not contain a won-ba:coordinationMessage URI");
            return;
          }

          Resource coordMsg = coordMsgNode.asResource();
          String sCoordMsg = coordMsg.toString(); //URI

          // URI -> eventType
          BAPCEventType eventType = BAPCEventType.getCoordinationEventTypeFromURI(sCoordMsg);

          BAPCState state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
          logger.debug("Current state of the Coordinator: "+state.getURI().toString());


          // atomic outcome
          // if one of the Participants votes NO (either EXIT or FAIL or CANNOTCOMPLETE is received)
          //ACTITVE  -> (message: CANCEL) -> CANCELING
          //COMPLETED -> (message: COMPENSATE) -> COMPENSATING
          if(isCoordinatorInFirstPhase(con) && (eventType.getURI().toString().equals(WON_BA.MESSAGE_EXIT.getURI().toString())) ||
            eventType.getURI().toString().equals(WON_BA.MESSAGE_FAIL.getURI().toString()) ||
            eventType.getURI().toString().equals(WON_BA.MESSAGE_CANNOTCOMPLETE.getURI().toString()))
          {
            state = state.transit(eventType);
            state.setPhase(BAPCState.Phase.SECOND); // The second phase is beginning
            stateManager.setStateForNeedUri(state, con.getNeedURI(), con.getRemoteNeedURI());
            logger.debug("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
            logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

            ownerFacingConnectionClient.textMessage(con.getConnectionURI(), message);

            logger.debug("Vote: NO ({})", eventType.getURI().toString());
            //new Participants can not be added anymore
            Model myContent = ModelFactory.createDefaultModel();
            myContent.setNsPrefix("","no:uri");
            Resource baseResource = myContent.createResource("no:uri");

            //list all Coordinator-Participant State Machines
            List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
            URI ownerUri = null;
            URI needUri = null;

            for(Connection tmpCon : listOfCons)
            {
              ownerUri = tmpCon.getNeedURI();
              needUri = tmpCon.getRemoteNeedURI();

              //process ACTIVE states
              if(stateManager.getStateForNeedUri(ownerUri, needUri).getURI().toString().equals(WON_BA.STATE_ACTIVE.getURI().toString()))
              {
                // Send MESSAGE_CANCEL to Participant
                state = stateManager.getStateForNeedUri(ownerUri, needUri);  //must be Active
                logger.debug("Current state of the Coordinator must be ACTIVE: "+state.getURI().toString());
                logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());
                eventType = BAPCEventType.getBAEventTypeFromURI(WON_BA.MESSAGE_CANCEL.getURI().toString());

                state = state.transit(eventType);       //propagate the phase, it is better to have this global
                state.setPhase(BAPCState.Phase.SECOND);
                stateManager.setStateForNeedUri(state, ownerUri, needUri);
                logger.debug("New state of the Coordinator must be CANCELING:"+stateManager.getStateForNeedUri(ownerUri, needUri));
                logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

                Resource r = myContent.createResource(BAPCEventType.MESSAGE_CANCEL.getURI().toString());
                baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                needFacingConnectionClient.textMessage(tmpCon, myContent);
                baseResource.removeAll(WON_BA.COORDINATION_MESSAGE) ;
              }
              //process COMPLETED state
              else if(stateManager.getStateForNeedUri(ownerUri, needUri).getURI().toString().equals(WON_BA.STATE_COMPLETED.getURI().toString()))
              {

                // Send MESSAGE_COMPENSATE to Participant
                state = stateManager.getStateForNeedUri(ownerUri, needUri);  //must be Active
                logger.debug("Current state of the Coordinator must be COMPLETED. The state is: {}", state.getURI().toString());
                logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());
                eventType = BAPCEventType.getBAEventTypeFromURI(WON_BA.MESSAGE_COMPENSATE.getURI().toString());

                state = state.transit(eventType);       //propagate the phase, it is better to have this global
                state.setPhase(BAPCState.Phase.SECOND);
                stateManager.setStateForNeedUri(state, ownerUri, needUri);
                logger.debug("New state of the Coordinator must be COMPENSATING. The state is: {}", stateManager.getStateForNeedUri(ownerUri, needUri));
                logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

                Resource r = myContent.createResource(BAPCEventType.MESSAGE_COMPENSATE.getURI().toString());
                baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                needFacingConnectionClient.textMessage(tmpCon, myContent);
                baseResource.removeAll(WON_BA.COORDINATION_MESSAGE) ;
              }
            }
          }
          else
          {
            BAPCState newState = state.transit(eventType);   //propagate the phase, it is better to have this global
            newState.setPhase(state.getPhase());
            stateManager.setStateForNeedUri(newState, con.getNeedURI(), con.getRemoteNeedURI());
            logger.debug("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
            logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

            ownerFacingConnectionClient.textMessage(con.getConnectionURI(), message);
          }

          BAPCEventType resendEventType = state.getResendEvent();
          if(resendEventType!=null)
          {
            Model myContent = ModelFactory.createDefaultModel();
            myContent.setNsPrefix("","no:uri");
            Resource baseResource = myContent.createResource("no:uri");

            if(BAPCEventType.isBAPCCoordinatorEventType(resendEventType))
            {
              state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
              BAPCState newState = null;
              logger.debug("Coordinator re-sends the previous message.");
              logger.debug("Current state of the Coordinator: "+state.getURI().toString());

              newState = state.transit(eventType);  //propagate the phase, it is better to have this global
              newState.setPhase(state.getPhase());
              stateManager.setStateForNeedUri(newState, con.getNeedURI(), con.getRemoteNeedURI());
              logger.debug("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
              logger.debug("Coordinator state indicator: {}", stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()).getPhase());

              // eventType -> URI Resource
              Resource r = myContent.createResource(resendEventType.getURI().toString());
              baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
              needFacingConnectionClient.textMessage(con, myContent);
            }
            else
            {
              logger.debug("The eventType: "+eventType.getURI().toString()+" can not be triggered by " +
                             "Participant.");
            }
          }
        } catch (WonProtocolException e) {
          logger.warn("caught WonProtocolException:", e);
        } catch (Exception e) {
          logger.debug("caught Exception",e);
        }

      }
    });
  }

  private boolean canFirstPhaseFinish(Connection con)
  {
    List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);

    URI ownerUri = null;
    URI needUri = null;
    URI currentStateUri = null;
    for(Connection tmpCon: listOfCons)
    {
      ownerUri = tmpCon.getNeedURI();
      needUri = tmpCon.getRemoteNeedURI();
      currentStateUri = stateManager.getStateForNeedUri(ownerUri, needUri).getURI();
      if(currentStateUri.toString().equals(WON_BA.STATE_ACTIVE.getURI().toString()))
        return false;
    }
    return true;
  }

  private List<Connection> getAllCoordinatorParticipantConnections(Connection con){
    List<Connection> listOfCons = connectionRepository.findByNeedURIAndStateAndTypeURI
      (con.getNeedURI(), ConnectionState.CONNECTED, FacetType.BAAtomicPCCoordinatorFacet.getURI());
    return listOfCons;
  }

  private boolean isCoordinatorInFirstPhase(Connection con)
  {
    List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
    URI ownerUri, needUri;
    BAPCState currentState;
    for(Connection c: listOfCons)
    {
      ownerUri = c.getNeedURI();
      needUri = c.getRemoteNeedURI();
      currentState = stateManager.getStateForNeedUri(ownerUri, needUri);
      if(currentState.getPhase()== BAPCState.Phase.SECOND)
        return false;
    }
    return true;
  }

  private void propagateSecondPhase(Connection con){
    List<Connection> listOfCons = getAllCoordinatorParticipantConnections(con);
    BAPCState state;
    for(Connection currentCon : listOfCons)
    {
      state = stateManager.getStateForNeedUri(currentCon.getNeedURI(), currentCon.getRemoteNeedURI());
      if(state.getPhase() == BAPCState.Phase.FIRST)
      {
        state.setPhase(BAPCState.Phase.SECOND);
        stateManager.setStateForNeedUri(state,currentCon.getNeedURI(), currentCon.getRemoteNeedURI());
      }
    }
  }
}