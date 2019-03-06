package won.bot.framework.eventbot.listener.impl;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.bot.context.ParticipantCoordinatorBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.bot.framework.eventbot.listener.baStateBots.SimpleScriptManager;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 26.2.14.
 * Time: 15.23
 * To change this template use File | Settings | File Templates.
 */
public class AutomaticBAMessageResponderListener extends BaseEventListener
{
    private long millisTimeoutBeforeReply = 1000;
    private SimpleScriptManager scriptManager = new SimpleScriptManager();
    private List<BATestBotScript> scripts;
    private int finishedScripts;
    private int messagesSentAndNotReceived = 0;
    private Object monitor = new Object();

    public AutomaticBAMessageResponderListener(final EventListenerContext context, final List<BATestBotScript> scripts, final long millisTimeoutBeforeReply)
    {
        super(context);
        this.scripts = scripts;
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

  @Override
  public String toString() {
    return "AutomaticBAMessageResponderListener{" +
      "finishedScripts=" + finishedScripts + "/" +scripts.size() +
      '}';
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
        logger.info("Are we on Coordinator side? "+weAreOnCoordinatorSide);
        BATestBotScript script = setupStateMachine(openEvent, weAreOnCoordinatorSide);
        //now, getGeneric the action from the state machine and make the right need send the message
        if (isScriptFinished(script)) return;
        BATestScriptAction action = script.getNextAction();


      //  logger.info("State of the sender before sending: {} ", action.getStateOfSenderBeforeSending());
        //decide from which need to send the initial WS-BA protocol message
        URI connectionToSendMessageFrom = determineConnectionToSendFrom(openEvent.getCon(), weAreOnCoordinatorSide, action);
        //2 sendMessageFromConnection(action, connectionToSendMessageFrom);
        sendModelFromConnection(action, connectionToSendMessageFrom);
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
            logger.debug("finished scripts: {}/{}, messages sent and not received {}", new Object[]{finishedScripts,
                                                                                                    scripts.size(),
                                                                                                    messagesSentAndNotReceived});
            if (this.finishedScripts == this.scripts.size() && messagesSentAndNotReceived == 0){
                publishFinishedEvent();
            }
        }
    }



    private void sendModelFromConnection(final BATestScriptAction action, URI connectionToSendMessageFrom) {

        final Model myContent = action.getMessageToBeSent();


        logger.info("Sent RDF: "+myContent);
        final URI senderURI = connectionToSendMessageFrom;
        getEventListenerContext().getTaskScheduler().schedule(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                  logger.info("State of the sender before sending: {} ", action.getStateOfSenderBeforeSending());
                    throw new UnsupportedOperationException("Not yet adapted to new message format!");
                    //getEventListenerContext().getWonMessageSender().sendWonMessage(senderURI, myContent, null);
                } catch (Exception e) {
                    logger.warn("could not send message via connection {}", senderURI, e);
                }
            }
        }, new Date(System.currentTimeMillis() + millisTimeoutBeforeReply));
    }

    private URI determineConnectionToSendFrom(Connection con, boolean weAreOnCoordinatorSide, BATestScriptAction action) {
      assert con != null : "Connection must not be null";
      assert action != null : "Action must not be null";
        URI connectionToSendMessageFrom = null;
        if (weAreOnCoordinatorSide && action.isSenderIsCoordinator()
                || !weAreOnCoordinatorSide && action.isSenderIsParticipant()){
            connectionToSendMessageFrom = con.getConnectionURI();
        } else {
          connectionToSendMessageFrom = WonLinkedDataUtils.getRemoteConnectionURIforConnectionURI(con
            .getConnectionURI(), getEventListenerContext().getLinkedDataSource());
          //TODO: also getGeneric BA state for participant/coordinator and compare to
          //state that the action says it should be in. If the state is different
          //throw an exception.
        }
      logger.debug("Are we on Coordinator side?" + weAreOnCoordinatorSide);
      logger.debug("Input connection:" + con);
      logger.debug("Return connection:" + connectionToSendMessageFrom);
      return connectionToSendMessageFrom;
    }

    private BATestBotScript setupStateMachine(OpenFromOtherNeedEvent openEvent, boolean weAreOnCoordinatorSide) {
        //create a key for the state machine manager:
        URI localNeedUri = openEvent.getCon().getNeedURI();
        URI remoteNeedUri = openEvent.getCon().getRemoteNeedURI();
        URI coordinatorUri = weAreOnCoordinatorSide? localNeedUri: remoteNeedUri;
        URI participantUri = weAreOnCoordinatorSide? remoteNeedUri: localNeedUri;
        //decide which state machine to use for this combination:

        ParticipantCoordinatorBotContextWrapper botContextWrapper = (ParticipantCoordinatorBotContextWrapper) getEventListenerContext().getBotContextWrapper();
        List<URI> participants = botContextWrapper.getParticipants();

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
        return (con.getTypeURI().equals(FacetType.BACCCoordinatorFacet.getURI()) || con.getTypeURI().equals(FacetType.BAPCCoordinatorFacet.getURI()) ||
        con.getTypeURI().equals(FacetType.BAAtomicCCCoordinatorFacet.getURI()) || con.getTypeURI().equals(FacetType.BAAtomicPCCoordinatorFacet.getURI()));
    }

    private void handleMessageEvent(final MessageFromOtherNeedEvent messageEvent){
        logger.debug("handleMessage: got message event for need: {}, connection state is: {}", messageEvent.getCon().getNeedURI(), messageEvent.getCon().getState());
        if (messageEvent.getCon().getState() != ConnectionState.CONNECTED){
            logger.warn("connection state must be CONNECTED when open is received. We don't expect open of connections created through hints.");
        }
        logger.debug("replying to open with message");
        //register a WS-BA state machine for this need-need combination:
        boolean weAreOnCoordinatorSide = areWeOnCoordinatorSide(messageEvent.getCon());
        //now, getGeneric the action from the state machine and make the right need send the message
        BATestBotScript script = getStateMachine(messageEvent.getCon(),weAreOnCoordinatorSide);
        if (isScriptFinished(script)) return;
        BATestScriptAction action = script.getNextAction();

        //decide from which need to send the initial WS-BA protocol message
        URI connectionToSendMessageFrom = determineConnectionToSendFrom(messageEvent.getCon(), weAreOnCoordinatorSide, action);
     //2   sendMessageFromConnection(action, connectionToSendMessageFrom);
        sendModelFromConnection(action, connectionToSendMessageFrom);
    }

 /*   private void unsubscribe()
    {
        logger.debug("unsubscribing from MessageFromOtherNeedEvent");
        getEventListenerContext().getEventBus().unsubscribe(this);
    } */

}



