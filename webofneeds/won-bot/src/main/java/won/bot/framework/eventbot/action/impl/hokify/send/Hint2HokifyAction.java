package won.bot.framework.eventbot.action.impl.hokify.send;

import won.bot.framework.bot.context.HokifyJobBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.hokify.WonHokifyJobBotHandler;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.Match;

import java.net.URI;

/**
 * Created by MS on 24.09.2018.
 */
public class Hint2HokifyAction extends BaseEventBotAction {
  WonHokifyJobBotHandler wonHokifyJobBotHandler;

  public Hint2HokifyAction(EventListenerContext ctx) {
    super(ctx);
  }

  @Override
  protected void doRun(Event event, EventListener executingListener) throws Exception {

    logger.info("HintEvent received");

    EventListenerContext ctx = getEventListenerContext();

    if (event instanceof HintFromMatcherEvent && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
      HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();
      Match match = ((HintFromMatcherEvent) event).getMatch();
      WonMessage wonMessage = ((HintFromMatcherEvent) event).getWonMessage();

      URI yourNeedUri = match.getFromNeed();
      URI remoteNeedUri = match.getToNeed();

      String jobURL = botContextWrapper.getJobURLForURI(yourNeedUri);
      if (jobURL == null) {
        logger.error("No JobURL found for the specified needUri");
        return;
      }

      try {
        // Message message =
        // wonHokifyJobBotHandler.sendMessage(wonHokifyJobBotHandler.getHokifyMessageGenerator().getHintMessage(remoteNeedUri,
        // yourNeedUri));
        // botContextWrapper.addMessageIdWonURIRelation(wonMessage.getMessageURI(), new
        // WonURI(wonMessage.getReceiverURI(), UriType.CONNECTION));
      } catch (Exception te) {
        logger.error("HERE is the Hint Exception" + te.getMessage());
      }
    }
  }
}
