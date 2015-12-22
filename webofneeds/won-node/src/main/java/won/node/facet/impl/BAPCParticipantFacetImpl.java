package won.node.facet.impl;


import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.businessactivity.participantcompletion.BAPCEventType;
import won.node.facet.businessactivity.participantcompletion.BAPCState;
import won.node.facet.businessactivity.statemanager.BAStateManager;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.WonProtocolException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 16.1.14.
 * Time: 16.30
 * To change this template use File | Settings | File Templates.
 */
public class BAPCParticipantFacetImpl extends AbstractBAFacet
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private BAStateManager stateManager;

    @Override
    public FacetType getFacetType() {
        return FacetType.BAPCParticipantFacet;
    }

    // Participant -> accept
    public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //inform the other side
        if (con.getRemoteConnectionURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        logger.debug("*** Participant sends open");

                      //TODO: use new system
                      // needFacingConnectionClient.open(con, content, wonMessage);
                        //needFacingConnectionClient.open(con.getRemoteConnectionURI(), content);

                        stateManager.setStateForNeedUri(BAPCState.ACTIVE.getURI(), con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI());
                        storeBAStateForConnection(con, BAPCState.ACTIVE.getURI());
                    } catch (Exception e) {
                        logger.debug("caught Exception",e);
                    }
                }
            });
        }
    }

    // Participant sends message to Coordinator
    public void sendMessageFromOwner(final Connection con, final Model message, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
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
                    messageForSending = WonRdfUtils.MessageUtils.getTextMessage(message);
                    if(messageForSending != null)
                    {
                        eventType = BAPCEventType.getCoordinationEventTypeFromString(messageForSending);
                        logger.debug("*** Participant sends the text message:"+eventType+" coordinator:"+con.getRemoteNeedURI()+ " participant:"+con.getNeedURI()+
                        " con:" +  con.getConnectionURI()+ " baState:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());
                    }
                    // message as MODEL
                    else {
                        NodeIterator ni = message.listObjectsOfProperty(message.getProperty(WON_TX.COORDINATION_MESSAGE
                          .getURI().toString()));
                        if(ni.hasNext())
                        {
                            String eventTypeURI = ni.toList().get(0).asResource().getURI().toString();
                            eventType = BAPCEventType.getBAEventTypeFromURI(eventTypeURI);

                            logger.debug("*** Participant sends the RDF message:"+eventType.getURI()+" coordinator:"+con.getRemoteNeedURI()+ " participant:"+con.getNeedURI()+
                            " con:" +  con.getConnectionURI()+ " baState:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());
                        }
                    }

                    myContent = ModelFactory.createDefaultModel();
                    myContent.setNsPrefix("","no:uri");
                    Resource baseResource = myContent.createResource("no:uri");

                    // message -> eventType
                    if((eventType!=null))
                    {
                        if(eventType.isBAPCParticipantEventType(eventType))
                        {
                            BAPCState state, newState;
                            state = BAPCState.parseString(stateManager.getStateForNeedUri(con.getNeedURI(),
                              con.getRemoteNeedURI(), getFacetType().getURI()).toString());

                            newState = state.transit(eventType);
                            stateManager.setStateForNeedUri(newState.getURI(), con.getNeedURI(),
                              con.getRemoteNeedURI(), getFacetType().getURI());
                            storeBAStateForConnection(con, newState.getURI());

                            logger.debug("New state = coordinator:"+con.getRemoteNeedURI()+ " participant:"+con.getNeedURI()+
                            " con:" +  con.getConnectionURI()+ " baState:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());

                            // eventType -> URI Resource
                            r = myContent.createResource(eventType.getURI().toString());
                            baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);

                          //TODO: use new system
                          // needFacingConnectionClient.sendMessage(con, myContent, wonMessage);

                            logger.debug("Participant sent the message.");
                        }
                        else
                        {
                            logger.info("The eventType: {} can not be triggered by Participant.", eventType.getURI().toString());
                        }
                    }
                    else
                    {
                        logger.info("The event type denoted by {} is not allowed.", messageForSending);
                    }
                } catch (Exception e) {
                    logger.debug("caught Exception", e);
                }
            }
        });
    }

    // Participant receives message from Coordinator
    public void sendMessageFromNeed(final Connection con, final Model message, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //send to the need side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    logger.debug("*** Participant receives a message:"+message.toString()+" coordinator:"+con.getRemoteNeedURI()+ " participant:"+con.getNeedURI()+
                    " con:" +  con.getConnectionURI()+ " baState:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());

                    NodeIterator it = message.listObjectsOfProperty(WON_TX.COORDINATION_MESSAGE);
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
                    BAPCEventType eventType = BAPCEventType.getCoordinationEventTypeFromURI(sCoordMsg);

                    BAPCState state, newState;
                    state = BAPCState.parseString(stateManager.getStateForNeedUri(con.getNeedURI(),
                      con.getRemoteNeedURI(), getFacetType().getURI()).toString());

                    newState = state.transit(eventType);
                    stateManager.setStateForNeedUri(newState.getURI(), con.getNeedURI(),
                      con.getRemoteNeedURI(), getFacetType().getURI());
                    storeBAStateForConnection(con, newState.getURI());

                    logger.debug("New state = coordinator:"+con.getRemoteNeedURI()+ " participant:"+con.getNeedURI()+
                      " con:" +  con.getConnectionURI()+ " baState:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()).toString());

                  //TODO: use new system
                  // ownerFacingConnectionClient.sendMessage(con.getConnectionURI(), message, wonMessage);

                    logger.debug("The message is received");

                    BAPCEventType resendEventType = state.getResendEvent();
                    if(resendEventType!=null)
                    {
                        logger.debug("Resending!!!");
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("","no:uri");
                        Resource baseResource = myContent.createResource("no:uri");

                        if(BAPCEventType.isBAPCParticipantEventType(resendEventType))
                        {
                            state = BAPCState.parseString(stateManager.getStateForNeedUri(con.getNeedURI(),
                              con.getRemoteNeedURI(), getFacetType().getURI()).toString());
                            logger.info("Participant re-sends the previous message.");
                            logger.info("Current state of the Participant: "+state.getURI().toString());
                            newState = state.transit(resendEventType);
                            stateManager.setStateForNeedUri(newState.getURI(), con.getNeedURI(),
                              con.getRemoteNeedURI(), getFacetType().getURI());
                            storeBAStateForConnection(con, newState.getURI());

                            logger.info("New state of the Participant:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI(), getFacetType().getURI()));

                            // eventType -> URI Resource
                            Resource r = myContent.createResource(resendEventType.getURI().toString());
                            baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, r);
                          //TODO: use new system
                          // needFacingConnectionClient.sendMessage(con, myContent, wonMessage);
                        }
                        else
                        {
                            logger.info("The eventType: "+eventType.getURI().toString()+" can not be triggered by Participant.");
                        }

                    }
                } catch (Exception e) {
                    logger.debug("caught Exception",e);
                }

            }
        });
    }

    public void setStateManager(final BAStateManager stateManager) {
      this.stateManager = stateManager;
    }
}
