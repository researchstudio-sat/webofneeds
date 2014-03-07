package won.bot.framework.events.listener;

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import won.bot.framework.events.Event;
import won.bot.framework.events.event.InternalWorkDoneEvent;
import won.bot.framework.events.event.MessageFromOtherNeedEvent;
import won.bot.framework.events.event.OpenFromOtherNeedEvent;
import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.SimpleScriptManager;
import won.bot.impl.BACCBot;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 26.2.14.
 * Time: 15.23
 * To change this template use File | Settings | File Templates.
 */
public class AutomaticBAMessageResponderListener extends BaseEventListener {
    private int targetNumberOfMessages = -1;
    private int numberOfMessagesSent = 0;
    private long millisTimeoutBeforeReply = 1000;
    private SimpleScriptManager scriptManager = new SimpleScriptManager();
    private List<BATestBotScript> scripts;
    private int finishedScripts;
    private Object monitor = new Object();
    LinkedDataRestClient client = new LinkedDataRestClient();
    public AutomaticBAMessageResponderListener(final EventListenerContext context, final List<BATestBotScript> scripts, final int targetNumberOfMessages, final long millisTimeoutBeforeReply)
    {
        super(context);
        this.scripts = scripts;
        this.targetNumberOfMessages = targetNumberOfMessages;
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }

    @Override
    public void doOnEvent(final Event event) throws Exception
    {
        if (event instanceof MessageFromOtherNeedEvent){
            handleMessageEvent((MessageFromOtherNeedEvent) event);
        } else if (event instanceof OpenFromOtherNeedEvent) {
            handleOpenEvent((OpenFromOtherNeedEvent) event);
        }

    }

    /**
     * React to open event by sending a message.
     *
     * @param openEvent
     */
    private void handleOpenEvent(final OpenFromOtherNeedEvent openEvent)
    {
        logger.debug("handleOpen: got open event for need: {}, connection state is: {}", openEvent.getCon().getNeedURI(), openEvent.getCon().getState());
        if (openEvent.getCon().getState() != ConnectionState.CONNECTED){
            logger.warn("connection state must be CONNECTED when open is received. We don't expect open of connections created through hints.");
        }
        logger.debug("replying to open with message");
        //register a WS-BA state machine for this need-need combination:
        boolean weAreOnCoordinatorSide = areWeOnCoordinatorSide(openEvent.getCon());
        BATestBotScript script = setupStateMachine(openEvent, weAreOnCoordinatorSide);
        //now, get the action from the state machine and make the right need send the message
        if (isScriptFinished(script)) return;
        BATestScriptAction action = script.getNextAction();

      //  logger.info("State of the sender before sending: {} ", action.getStateOfSenderBeforeSending());
        //decide from which need to send the initial WS-BA protocol message
        URI connectionToSendMessageFrom = determineConnectionToSendFrom(openEvent.getCon(), weAreOnCoordinatorSide, action);
        sendMessageFromConnection(action, connectionToSendMessageFrom);
    }

    private boolean isScriptFinished(BATestBotScript script) {
        synchronized (monitor){
            if (!script.hasNext()){
                this.finishedScripts++;
                publishFinishedEventIfFinished();
                return true;
            }
        }
        return false;
    }

    private void publishFinishedEventIfFinished() {
        synchronized (monitor){
            if (this.finishedScripts == this.scripts.size()){
                InternalWorkDoneEvent finishedEvent = new InternalWorkDoneEvent();
                getEventListenerContext().getEventBus().publish(finishedEvent);
            }
        }
    }

    private void sendMessageFromConnection(final BATestScriptAction action, URI connectionToSendMessageFrom) {

        final String message = action.getMessageToBeSent();
        final URI senderURI = connectionToSendMessageFrom;
        getEventListenerContext().getTaskScheduler().schedule(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    getEventListenerContext().getOwnerService().textMessage(senderURI, WonRdfUtils.MessageUtils.textMessage(message));
                    logger.info("State of the sender before sending: {} ", action.getStateOfSenderBeforeSending());
                } catch (Exception e) {
                    logger.warn("could not send message via connection {}", senderURI, e);
                }
         }
                    }, new Date(System.currentTimeMillis() + millisTimeoutBeforeReply));
    }

    private URI determineConnectionToSendFrom(Connection con, boolean weAreOnCoordinatorSide, BATestScriptAction action) {
        URI connectionToSendMessageFrom = null;
        if (weAreOnCoordinatorSide && action.isSenderIsCoordinator()
                || !weAreOnCoordinatorSide && action.isSenderIsParticipant()){
            connectionToSendMessageFrom = con.getConnectionURI();
        } else {
            Path propertyPath = PathParser.parse("<"+ WON.HAS_REMOTE_CONNECTION+">", PrefixMapping.Standard);
            connectionToSendMessageFrom = client.getURIPropertyForPropertyPath(con.getConnectionURI(), propertyPath);
            //TODO: also get BA state for participant/coordinator and compare to
            //state that the action says it should be in. If the state is different
            //throw an exception.
        }
        logger.debug("Are we on Coordinator side?"+weAreOnCoordinatorSide);
        logger.debug("Input connection:"+con);
        logger.debug("Return connection:"+connectionToSendMessageFrom);
        return connectionToSendMessageFrom;
    }

    private BATestBotScript setupStateMachine(OpenFromOtherNeedEvent openEvent, boolean weAreOnCoordinatorSide) {
        //create a key for the state machine manager:
        URI localNeedUri = openEvent.getCon().getNeedURI();
        URI remoteNeedUri = openEvent.getCon().getRemoteNeedURI();
        URI coordinatorUri = weAreOnCoordinatorSide? localNeedUri: remoteNeedUri;
        URI participantUri = weAreOnCoordinatorSide? remoteNeedUri: localNeedUri;
        //decide which state machine to use for this combination:
        List<URI> participants = getEventListenerContext().getBotContext().getNamedNeedUriList(BACCBot.URI_LIST_NAME_PARTICIPANT);
        logger.debug("participants:{}", participants);
        logger.debug("participant URI to look for:{}", participantUri);
        //let's hard-code this for now:
        BATestBotScript stateMachine = null;
        stateMachine = scripts.get(participants.indexOf(participantUri));
        scriptManager.setStateForNeedUri(stateMachine, coordinatorUri, participantUri);
        return stateMachine;
    }

    private BATestBotScript getStateMachine(Connection con, boolean weAreOnCoordinatorSide) {
        //create a key for the state machine manager:
        URI localNeedUri = con.getNeedURI();
        URI remoteNeedUri = con.getRemoteNeedURI();
        URI coordinatorUri = weAreOnCoordinatorSide? localNeedUri: remoteNeedUri;
        URI participantUri = weAreOnCoordinatorSide? remoteNeedUri: localNeedUri;
        return scriptManager.getStateForNeedUri(coordinatorUri, participantUri);
    }

    private boolean areWeOnCoordinatorSide(Connection con) {
        return (con.getTypeURI().equals(FacetType.BACCCoordinatorFacet.getURI()) || con.getTypeURI().equals(FacetType.BAPCCoordinatorFacet.getURI()));
    }

    private void handleMessageEvent(final MessageFromOtherNeedEvent messageEvent){
        logger.debug("handleMessage: got message event for need: {}, connection state is: {}", messageEvent.getCon().getNeedURI(), messageEvent.getCon().getState());
        if (messageEvent.getCon().getState() != ConnectionState.CONNECTED){
            logger.warn("connection state must be CONNECTED when open is received. We don't expect open of connections created through hints.");
        }
        logger.debug("replying to open with message");
        //register a WS-BA state machine for this need-need combination:
        boolean weAreOnCoordinatorSide = areWeOnCoordinatorSide(messageEvent.getCon());
        //now, get the action from the state machine and make the right need send the message
        BATestBotScript script = getStateMachine(messageEvent.getCon(),weAreOnCoordinatorSide);
        if (isScriptFinished(script)) return;
        BATestScriptAction action = script.getNextAction();

        //decide from which need to send the initial WS-BA protocol message
        URI connectionToSendMessageFrom = determineConnectionToSendFrom(messageEvent.getCon(), weAreOnCoordinatorSide, action);
        sendMessageFromConnection(action, connectionToSendMessageFrom);
    }

 /*   private void unsubscribe()
    {
        logger.debug("unsubscribing from MessageFromOtherNeedEvent");
        getEventListenerContext().getEventBus().unsubscribe(this);
    } */

}



