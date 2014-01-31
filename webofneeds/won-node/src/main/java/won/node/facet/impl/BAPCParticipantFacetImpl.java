package won.node.facet.impl;



import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.businessactivity.*;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;

import won.node.facet.businessactivity.BAStateManager;
import won.node.facet.businessactivity.SimpleBAStateManager;
import won.node.facet.businessactivity.BAEventType;
import won.node.facet.businessactivity.BAParticipantCompletionState;


import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 16.1.14.
 * Time: 16.30
 * To change this template use File | Settings | File Templates.
 */
public class BAPCParticipantFacetImpl extends Facet{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConnectionRepository connectionRepository;
    private SimpleBAStateManager stateManager = new SimpleBAStateManager();

    @Override
    public FacetType getFacetType() {
        return FacetType.BAPCParticipantFacet;
    }

    // particiapant -> accept
    public void openFromOwner(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //inform the other side
        if (con.getRemoteConnectionURI() != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        needFacingConnectionClient.open(con, content);
                        //needFacingConnectionClient.open(con.getRemoteConnectionURI(), content);

                        stateManager.setStateForNeedUri(BAParticipantCompletionState.ACTIVE, con.getNeedURI());
                        logger.info("Participant state: "+stateManager.getStateForNeedUri(con.getNeedURI()));
                    } catch (WonProtocolException e) {
                        logger.debug("caught Exception:", e);
                    }
                }
            });
        }
    }




    public void textMessageFromOwner(final Connection con, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        final URI remoteConnectionURI = con.getRemoteConnectionURI();
       // System.out.println("daki Poziva: "+"Participant textMessageFromOwner");


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
                    //System.out.println("daki: Participant sents:"+message.toString());

                    messageForSending = ni.toList().get(0).toString();
                    messageForSending = messageForSending.substring(0, messageForSending.indexOf("^^http:"));
                    logger.info("Participant sents: " + messageForSending);

                    myContent = ModelFactory.createDefaultModel();
                    myContent.setNsPrefix("","no:uri");
                    Resource baseResource = myContent.createResource("no:uri");

                    // message -> eventType
                    eventType = getCoordinationEventType(messageForSending);
                    if((eventType!=null))
                    {
                        if(eventType.isBAPCParticipantEventType(eventType))
                        {
                            eventType.isBAPCParticipantEventType(eventType);


                            BAParticipantCompletionState state = stateManager.getStateForNeedUri(con.getNeedURI());
                            logger.info("Current state of the Participant: "+state.getURI().toString());
                            stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI());
                            logger.info("New state of the Participant:"+stateManager.getStateForNeedUri(con.getNeedURI()));

                            // eventType -> URI Resource
                            r = myContent.createResource(eventType.getURI().toString());
                            baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, r);
                            //baseResource.addProperty(WON_BA.COORDINATION_MESSAGE, WON_BA.COORDINATION_MESSAGE_COMMIT);

                            needFacingConnectionClient.textMessage(con, myContent);
                        }
                        else
                        {
                            logger.info("The eventType: "+eventType.getURI().toString()+" can not be triggered by Participant.");
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

    public void textMessageFromNeed(final Connection con, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //send to the need side
        System.out.println("daki Poziva: "+"Participant textMessageFromNeed");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ownerFacingConnectionClient.textMessage(con.getConnectionURI(), message);
                } catch (WonProtocolException e) {
                    logger.warn("caught WonProtocolException:", e);
                }
            }
        });
    }

    public BAEventType getCoordinationEventType(final String fragment)
    {
        for (BAEventType event : BAEventType.values())
            if (event.name().equals(fragment))
            {
                return event;
            }
        return null;
    }





}
