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
        System.out.println("daki Coordinator: openFromNeed");
        executorService.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    ownerFacingConnectionClient.open(con.getConnectionURI(), content);

                    stateManager.setStateForNeedUri(BAParticipantCompletionState.ACTIVE, con.getNeedURI());
                    logger.info("Coordinator state: "+stateManager.getStateForNeedUri(con.getNeedURI()));
                } catch (WonProtocolException e) {
                    logger.debug("caught Exception:", e);
                }
            }
        });
    }

    public void textMessageFromOwner(final Connection con, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        final URI remoteConnectionURI = con.getRemoteConnectionURI();
        System.out.println("daki Poziva: "+"Coordinator textMessageFromOwner");
        //inform the other side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("daki: stanje:"+stateManager.getStateForNeedUri(con.getNeedURI()).toString());
                    needFacingConnectionClient.textMessage(con, message);
                    System.out.println("daki Con: con.getNeedURI():" + con.getNeedURI() + " con.remoteNeedURI():" + con.getRemoteNeedURI() +
                            " con.getConnectionURI():" + con.getConnectionURI() + " con.getRemoteConnectionURI:" + con.getRemoteConnectionURI() +
                            " con.getState():" + con.getState() + " con.getTypeURI():" + con.getTypeURI());
                   // NeedPojo n = new NeedPojo(con.getRemoteNeedURI(), null);

                   //System.out.println("daki message"+message.toString());
                     // message:
                    NodeIterator ni = message.listObjectsOfProperty(message.getProperty(WON_BA.BASE_URI,"hasTextMessage"));
                    System.out.println(ni.toList().get(0).toString());

                   /* StmtIterator iter = message.listStatements();
                    boolean found = false;
                    while(iter.hasNext() && !found)
                    {
                        Statement stmt = iter.nextStatement();  // get next statement
                        Property  predicate = stmt.getPredicate();   // get the predicate
                        if(predicate.toString().equals(WON_BA.BASE_URI+"hasTextMessage"))
                        {
                            System.out.print("Poruka je: " + stmt.getObject().toString() + " ");
                        }
                    }*/






                    // needFacingConnectionClient.textMessage(remoteConnectionURI, message);
                } catch (WonProtocolException e) {
                    logger.warn("caught WonProtocolException:", e);
                }
            }
        });
    }

    public void textMessageFromNeed(final Connection con, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //send to the need side
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("daki Received message from Participant: "+message.toString());
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
                    BAEventType eventType = getCoordinationEventType(sCoordMsg);
                    System.out.println("daki2: "+con.getNeedURI());
                    BAParticipantCompletionState state = stateManager.getStateForNeedUri(con.getNeedURI());
                    logger.info("Current state of the Coordinator: "+state.getURI().toString());
                    stateManager.setStateForNeedUri(state.transit(eventType), con.getNeedURI());
                    logger.info("New state of the Coordinator:"+stateManager.getStateForNeedUri(con.getNeedURI()));

                    ownerFacingConnectionClient.textMessage(con.getConnectionURI(), message);
                } catch (WonProtocolException e) {
                    logger.warn("caught WonProtocolException:", e);
                }

            }
        });
    }


    public BAEventType getCoordinationEventType(final String fragment)
    {
        String s = fragment.substring(fragment.lastIndexOf("#Message")+8,fragment.length());
       // System.out.println("daki: Poredi sa: "+s);
        for (BAEventType event : BAEventType.values())
            if (event.name().equals("MESSAGE_"+fragment.substring(fragment.lastIndexOf("#Message")+8,fragment.length()).toUpperCase()))
            {
                return event;
            }

        logger.warn("No enum could be matched for: {}", fragment);
        return null;
    }
}