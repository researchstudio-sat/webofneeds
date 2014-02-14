package won.node.facet.impl;



import com.hp.hpl.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.facet.businessactivity.BAStateManager;
import won.node.facet.businessactivity.SimpleBAStateManager;
import won.node.facet.businessactivity.BAEventType;
import won.node.facet.businessactivity.BAParticipantCompletionState;


import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;








import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 16.1.14.
 * Time: 16.39
 * To change this template use File | Settings | File Templates.
 */
public class BAPCCoordinatorFacetImpl extends Facet {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SimpleBAStateManager stateManager = new SimpleBAStateManager();


    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public FacetType getFacetType() {
        return FacetType.BAPCCoordinatorFacet;
    }

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

                    stateManager.setStateForNeedUri(BAParticipantCompletionState.ACTIVE, con.getNeedURI(), con.getRemoteNeedURI());
                    logger.info("Coordinator state: "+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
                } catch (WonProtocolException e) {
                    logger.debug("caught Exception:", e);
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
                    BAEventType eventType = null;
                    Model myContent = null;
                    Resource r = null;

                    //message (event) for sending
                    NodeIterator ni = message.listObjectsOfProperty(message.getProperty(WON_BA.BASE_URI,"hasTextMessage"));
                    //System.out.println("daki: Participant sends:"+message.toString());

                    messageForSending = ni.toList().get(0).toString();
                    messageForSending = messageForSending.substring(0, messageForSending.indexOf("^^http:"));
                    logger.info("Cooridnator sends: " + messageForSending);

                    myContent = ModelFactory.createDefaultModel();
                    myContent.setNsPrefix("","no:uri");
                    Resource baseResource = myContent.createResource("no:uri");

                    // message -> eventType
                    eventType = BAEventType.getCoordinationEventTypeFromString(messageForSending);
                    if((eventType!=null))
                    {
                        if(BAEventType.isBAPCCoordinatorEventType(eventType))
                        {
                            BAParticipantCompletionState state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                            logger.info("Current state of the Coordinator: "+state.getURI().toString());
                            stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI(), con.getRemoteNeedURI());
                            logger.info("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                            // eventType -> URI Resource
                            r = myContent.createResource(eventType.getURI().toString());
                            baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                            //baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, WON_BA.COORDINATION_MESSAGE_COMMIT);

                            needFacingConnectionClient.textMessage(con, myContent);
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
                    BAEventType eventType = BAEventType.getCoordinationEventTypeFromURI(sCoordMsg);

                    BAParticipantCompletionState state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                    logger.info("Current state of the Coordinator: "+state.getURI().toString());
                    stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI(), con.getRemoteNeedURI());
                    logger.info("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                    ownerFacingConnectionClient.textMessage(con.getConnectionURI(), message);
                    System.out.println("daki Nesto");


                    BAEventType resendEventType = state.getResendEvent();
                    if(resendEventType!=null)
                    {
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("","no:uri");
                        Resource baseResource = myContent.createResource("no:uri");

                        if(BAEventType.isBAPCCoordinatorEventType(resendEventType))
                        {
                            state = stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI());
                            logger.info("Coordinator re-sends the previous message.");
                            logger.info("Current state of the Coordinator: "+state.getURI().toString());
                            stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI(), con.getRemoteNeedURI());
                            logger.info("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                            // eventType -> URI Resource
                            Resource r = myContent.createResource(resendEventType.getURI().toString());
                            baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                            //baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, WON_BA.COORDINATION_MESSAGE_COMMIT);
                            needFacingConnectionClient.textMessage(con, myContent);
                        }
                        else
                        {
                            logger.info("The eventType: "+eventType.getURI().toString()+" can not be triggered by Participant.");
                        }
                    }
                } catch (WonProtocolException e) {
                    logger.warn("caught WonProtocolException:", e);
                }

            }
        });
    }
}