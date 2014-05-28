package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.businessactivity.BAStateManager;
import won.node.facet.businessactivity.coordinatorcompletion.BACCEventType;
import won.node.facet.businessactivity.coordinatorcompletion.BACCState;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.WonProtocolException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 6.2.14.
 * Time: 15.36
 * To change this template use File | Settings | File Templates.
 */
public class BACCCoordinatorFacetImpl extends AbstractFacet
{
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private BAStateManager stateManager;


    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public FacetType getFacetType() {
        return FacetType.BACCCoordinatorFacet;
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

                    stateManager.setStateForNeedUri(BACCState.ACTIVE.getURI(), con.getNeedURI(), con.getRemoteNeedURI());
                    logger.debug("Coordinator state: "+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));
                } catch (WonProtocolException e) {
                    logger.warn("caught Exception:", e);
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
                        logger.debug("Coordinator sends the text message: {}", eventType.getURI());
                    }

                    //message as MODEL
                    else {
                        ni = message.listObjectsOfProperty(message.getProperty(WON_BA.COORDINATION_MESSAGE.getURI().toString()));
                        if(ni.hasNext())
                        {
                            String eventTypeURI = ni.toList().get(0).asResource().getURI().toString();
                            eventType = BACCEventType.getBAEventTypeFromURI(eventTypeURI);
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
                        if(eventType.isBACCCoordinatorEventType(eventType))
                        {
                            BACCState state = BACCState.parseString(stateManager.getStateForNeedUri(con.getNeedURI(),
                              con.getRemoteNeedURI()).toString());
                            logger.debug("Current state of the Coordinator: "+state.getURI().toString());
                            stateManager.setStateForNeedUri(state.transit(eventType).getURI(), con.getNeedURI(),
                              con.getRemoteNeedURI());
                            logger.debug("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                            // eventType -> URI Resource
                            r = myContent.createResource(eventType.getURI().toString());
                            baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                            needFacingConnectionClient.textMessage(con, myContent);
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
                    logger.warn("caught Exception", e);
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
                    BACCEventType eventType = BACCEventType.getCoordinationEventTypeFromURI(sCoordMsg);

                    BACCState state = BACCState.parseString(stateManager.getStateForNeedUri(con.getNeedURI(),
                    con.getRemoteNeedURI()).toString());
                    logger.debug("Current state of the Coordinator: "+state.getURI().toString());
                    stateManager.setStateForNeedUri(state.transit(eventType).getURI(), con.getNeedURI(),
                      con.getRemoteNeedURI());
                    logger.debug("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                    ownerFacingConnectionClient.textMessage(con.getConnectionURI(), message);

                    BACCEventType resendEventType = state.getResendEvent();
                    if(resendEventType!=null)
                    {
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("","no:uri");
                        Resource baseResource = myContent.createResource("no:uri");

                        if(BACCEventType.isBACCCoordinatorEventType(resendEventType))
                        {
                            state = BACCState.parseString(stateManager.getStateForNeedUri(con.getNeedURI(),
                              con.getRemoteNeedURI()).toString());
                            logger.debug("Coordinator re-sends the previous message.");
                            logger.debug("Current state of the Coordinator: "+state.getURI().toString());
                            stateManager.setStateForNeedUri(state.transit(eventType).getURI(), con.getNeedURI(),
                              con.getRemoteNeedURI());
                            logger.debug("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI(), con.getRemoteNeedURI()));

                            // eventType -> URI Resource
                            Resource r = myContent.createResource(resendEventType.getURI().toString());
                            baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                            needFacingConnectionClient.textMessage(con, myContent);
                        }
                        else
                        {
                            logger.debug("The eventType: "+eventType.getURI().toString()+" can not be triggered by Participant.");
                        }
                    }
                } catch (WonProtocolException e) {
                    logger.warn("caught WonProtocolException:", e);
                } catch (Exception e) {
                    logger.warn("caught Exception",e);
                }

            }
        });
    }

    public void setStateManager(final BAStateManager stateManager) {
      this.stateManager = stateManager;
    }
}
