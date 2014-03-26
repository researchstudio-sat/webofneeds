package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.businessactivity.atomic.ATConnectionState;
import won.node.facet.businessactivity.atomic.SimpleATBAConnectionStateManager;
import won.node.facet.businessactivity.coordinatorcompletion.BACCEventType;
import won.node.facet.businessactivity.coordinatorcompletion.BACCState;
import won.node.facet.businessactivity.coordinatorcompletion.SimpleBACCStateManager;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;

/**
 * User: Danijel
 * Date: 26.3.14.
 */
public class BAAtomicCCCoordinatorFacetImpl extends Facet {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SimpleBACCStateManager stateManager = new SimpleBACCStateManager();
    private SimpleATBAConnectionStateManager connectionManager = new SimpleATBAConnectionStateManager();
    private Boolean firstPhase = true;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public FacetType getFacetType() {
        return FacetType.BAAtomicCCCoordinatorFacet;
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

                    stateManager.setStateForNeedUri(BACCState.ACTIVE, con.getNeedURI(), con.getRemoteNeedURI());
                    logger.info("Coordinator state: "+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
                } catch (WonProtocolException e) {
                    logger.debug("caught Exception:", e);
                }
                // adding Participant is possible only in the first phase
                if(firstPhase)  //add a new Participant (first phase in progress)
                {
                    stateManager.setStateForNeedUri(BACCState.ACTIVE, con.getNeedURI(), con.getRemoteNeedURI());
                    connectionManager.setStateForConnection(BACCState.ACTIVE.getURI(), con);
                    logger.info("Coordinator state: " + stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
                }
                else  // second phase in progress, new participants can not be added anymore
                {
                    logger.info("It is not possible to add more participants. The second phase of the protocol has already been started.");
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
                    BACCEventType eventType = null;
                    Model myContent = null;
                    Resource r = null;

                    //message (event) for sending

                    //message as TEXT
                    NodeIterator ni = message.listObjectsOfProperty(message.getProperty(WON_BA.BASE_URI,"hasTextMessage"));
                    if (ni.hasNext())
                    {
                        messageForSending = ni.toList().get(0).toString();
                        messageForSending = messageForSending.substring(0, messageForSending.indexOf("^^http:"));
                        eventType = BACCEventType.getCoordinationEventTypeFromString(messageForSending);
                        logger.info("Coordinator sends the text message: {}", eventType.getURI());
                    }

                    //message as MODEL
                    else {
                        ni = message.listObjectsOfProperty(message.getProperty(WON_BA.COORDINATION_MESSAGE.getURI().toString()));
                        if(ni.hasNext())
                        {
                            String eventTypeURI = ni.toList().get(0).asResource().getURI().toString();
                            eventType = BACCEventType.getBAEventTypeFromURI(eventTypeURI);
                            logger.info("Coordinator sends the RDF: {}", eventType.getURI());
                        }
                        else
                        {
                            logger.info("Message {} does not contain a proper content.", message.toString());
                            return;
                        }
                    }
                    myContent = ModelFactory.createDefaultModel();
                    myContent.setNsPrefix("","no:uri");
                    Resource baseResource = myContent.createResource("no:uri");

                    if((eventType!=null))
                    {
                        if(eventType.isBACCCoordinatorEventType(eventType))
                        {
                            if(firstPhase && eventType.getURI().toString().equals(WON_BA.MESSAGE_CLOSE.getURI().toString()))
                            {
                                //check whether all connections are in the state Completed (all participant voted with YES)
                                // if yes -> send message and firstPhase = false;
                                // if no -> wait for other participants to respond with Completed
                                if(canFirstPhaseFinish())
                                {
                                    BACCState state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                                    logger.info("Current state of the Coordinator: "+state.getURI().toString());
                                    stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI(), con.getRemoteNeedURI());
                                    connectionManager.setStateForConnection(state.transit(eventType).getURI(), con);
                                    logger.info("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                                    // eventType -> URI Resource
                                    r = myContent.createResource(eventType.getURI().toString());
                                    baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                                    needFacingConnectionClient.textMessage(con, myContent);

                                    firstPhase = false;
                                }
                                else
                                {
                                    logger.info("Not all votes are received from Participants!");
                                }
                                //TODO
                            }
                            else
                            {
                                BACCState state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                                logger.info("Current state of the Coordinator: "+state.getURI().toString());
                                stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI(), con.getRemoteNeedURI());
                                connectionManager.setStateForConnection(state.transit(eventType).getURI(), con);
                                logger.info("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                                // eventType -> URI Resource
                                r = myContent.createResource(eventType.getURI().toString());
                                baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                                needFacingConnectionClient.textMessage(con, myContent);
                            }
                        }
                        else
                        {
                            logger.info("The eventType: "+eventType.getURI().toString()+" can not be triggered by Coordinator.");
                        }
                    }
                    else
                    {
                        logger.info("The event type denoted by "+messageForSending+" is not allowed.");
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
                    logger.info("Received message from Participant: " + message.toString());
                    NodeIterator it = message.listObjectsOfProperty(WON_BA.COORDINATION_MESSAGE);
                    if (!it.hasNext()) {
                        logger.info("message did not contain a won-ba:coordinationMessage");
                        return;
                    }
                    RDFNode coordMsgNode = it.nextNode();
                    if (!coordMsgNode.isURIResource()){
                        logger.info("message did not contain a won-ba:coordinationMessage URI");
                        return;
                    }

                    Resource coordMsg = coordMsgNode.asResource();
                    String sCoordMsg = coordMsg.toString(); //URI

                    // URI -> eventType
                    BACCEventType eventType = BACCEventType.getCoordinationEventTypeFromURI(sCoordMsg);

                    BACCState state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                    logger.info("Current state of the Coordinator: "+state.getURI().toString());
                    stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI(), con.getRemoteNeedURI());
                    connectionManager.setStateForConnection(state.transit(eventType).getURI(), con);
                    logger.info("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                    ownerFacingConnectionClient.textMessage(con.getConnectionURI(), message);



                    // atomic outcome
                    // if one of the Participants votes NO (either EXIT or Fail or CannnotComplete is received)
                    //ACITVE -> (message: CANCEL) -> CANCELING
                    //COMPLETED -> (message: COMPENSATE) -> COMPENSATING

                    if(firstPhase && (eventType.getURI().toString().equals(WON_BA.MESSAGE_EXIT.getURI().toString())) ||
                            eventType.getURI().toString().equals(WON_BA.MESSAGE_FAIL.getURI().toString()) ||
                            eventType.getURI().toString().equals(WON_BA.MESSAGE_CANNOTCOMPLETE.getURI().toString()))
                    {
                        logger.info("Vote: NO ({})", eventType.getURI().toString());
                        firstPhase = false; //new Participants can not be added anymore
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("","no:uri");
                        Resource baseResource = myContent.createResource("no:uri");

                        //list all Coordinator-Participant State Machines
                        Iterator<Map.Entry<String,ATConnectionState>> mapIt = connectionManager.getMap().entrySet().iterator();
                        while (mapIt.hasNext())
                        {
                            ATConnectionState atConnectionState = mapIt.next().getValue();

                            //process Active state
                            if(atConnectionState.getOwnerStateUri().toString().equals(WON_BA.STATE_ACTIVE.getURI().toString()) ||
                                    atConnectionState.getOwnerStateUri().toString().equals(WON_BA.STATE_COMPLETING.getURI().toString()))
                            {
                                // Send MESSAGE_CANCEL to Participant
                                state = stateManager.getStateForNeedUri(atConnectionState.getCon().getNeedURI(), atConnectionState.getCon().getRemoteNeedURI());  //must be Active
                                logger.info("Current state of the Coordinator must be either ACTIVE or COMPLETING: "+state.getURI().toString());
                                eventType = BACCEventType.getBAEventTypeFromURI(WON_BA.MESSAGE_CANCEL.getURI().toString());
                                stateManager.setStateForNeedUri(state.transit(eventType), atConnectionState.getCon().getNeedURI(), atConnectionState.getCon().getRemoteNeedURI());
                                logger.info("New state of the Coordinator must be CANCELING:"+stateManager.getStateForNeedUri(atConnectionState.getCon().getNeedURI(), atConnectionState.getCon().getRemoteNeedURI()));
                                connectionManager.setStateForConnection(URI.create(WON_BA.STATE_CANCELING.getURI().toString()), atConnectionState.getCon());

                                Resource r = myContent.createResource(BACCEventType.MESSAGE_CANCEL.getURI().toString());
                                baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                                needFacingConnectionClient.textMessage(atConnectionState.getCon(), myContent);
                                baseResource.removeAll(WON_BA.COORDINATION_MESSAGE) ;
                            }
                            //process COMPLETED state
                            else if(atConnectionState.getOwnerStateUri().toString().equals(WON_BA.STATE_COMPLETED.getURI().toString()))
                            {

                                // Send MESSAGE_COMPENSATE to Participant
                                state = stateManager.getStateForNeedUri(atConnectionState.getCon().getNeedURI(), atConnectionState.getCon().getRemoteNeedURI());  //must be Active
                                logger.info("Current state of the Coordinator must be COMPLETED. The state is: {}", state.getURI().toString());
                                eventType = BACCEventType.getBAEventTypeFromURI(WON_BA.MESSAGE_COMPENSATE.getURI().toString());
                                stateManager.setStateForNeedUri(state.transit(eventType), atConnectionState.getCon().getNeedURI(), atConnectionState.getCon().getRemoteNeedURI());
                                logger.info("New state of the Coordinator must be COMPENSATING. The state is: {}", stateManager.getStateForNeedUri(atConnectionState.getCon().getNeedURI(), atConnectionState.getCon().getRemoteNeedURI()));
                                connectionManager.setStateForConnection(URI.create(WON_BA.STATE_COMPENSATING.getURI().toString()), atConnectionState.getCon());

                                Resource r = myContent.createResource(BACCEventType.MESSAGE_COMPENSATE.getURI().toString());
                                baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                                needFacingConnectionClient.textMessage(atConnectionState.getCon(), myContent);
                                baseResource.removeAll(WON_BA.COORDINATION_MESSAGE) ;
                            }
                        }
                    }


















                    BACCEventType resendEventType = state.getResendEvent();
                    if(resendEventType!=null)
                    {
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("","no:uri");
                        Resource baseResource = myContent.createResource("no:uri");

                        if(BACCEventType.isBACCCoordinatorEventType(resendEventType))
                        {
                            state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                            logger.info("Coordinator re-sends the previous message.");
                            logger.info("Current state of the Coordinator: "+state.getURI().toString());
                            stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI(), con.getRemoteNeedURI());
                            connectionManager.setStateForConnection(state.transit(eventType).getURI(), con);
                            logger.info("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                            // eventType -> URI Resource
                            Resource r = myContent.createResource(resendEventType.getURI().toString());
                            baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                            needFacingConnectionClient.textMessage(con, myContent);
                        }
                        else
                        {
                            logger.info("The eventType: "+eventType.getURI().toString()+" can not be triggered by Participant.");
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

    private boolean canFirstPhaseFinish()
    {
        boolean ret = true;

        Iterator<Map.Entry<String,ATConnectionState>> mapIt = connectionManager.getMap().entrySet().iterator();
        //is there any Coordinator  - Participant state machine in State != Completed
        while (mapIt.hasNext())
        {
            ATConnectionState atConnectionState = mapIt.next().getValue();
            if(ret && !atConnectionState.getOwnerStateUri().toString().equals(WON_BA.STATE_COMPLETED.getURI().toString()))
            {
                ret = false;
            }
        }
        return ret;
    }
}
