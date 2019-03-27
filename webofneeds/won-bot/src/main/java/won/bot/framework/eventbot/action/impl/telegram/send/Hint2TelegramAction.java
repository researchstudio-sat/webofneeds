package won.bot.framework.eventbot.action.impl.telegram.send;

import java.net.URI;

import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import won.bot.framework.bot.context.TelegramBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.Match;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Hint2TelegramAction extends BaseEventBotAction {
    WonTelegramBotHandler wonTelegramBotHandler;

    public Hint2TelegramAction(EventListenerContext ctx, WonTelegramBotHandler wonTelegramBotHandler) {
        super(ctx);
        this.wonTelegramBotHandler = wonTelegramBotHandler;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof HintFromMatcherEvent && ctx.getBotContextWrapper() instanceof TelegramBotContextWrapper) {
            TelegramBotContextWrapper botContextWrapper = (TelegramBotContextWrapper) ctx.getBotContextWrapper();
            Match match = ((HintFromMatcherEvent) event).getMatch();
            WonMessage wonMessage = ((HintFromMatcherEvent) event).getWonMessage();
            URI yourNeedUri = match.getFromNeed();
            URI remoteNeedUri = match.getToNeed();
            Long chatId = botContextWrapper.getChatIdForURI(yourNeedUri);
            if (chatId == null) {
                logger.error("No chatId found for the specified needUri");
                return;
            }
            try {
                Message message = wonTelegramBotHandler.sendMessage(wonTelegramBotHandler.getTelegramMessageGenerator()
                                .getHintMessage(chatId, remoteNeedUri, yourNeedUri));
                botContextWrapper.addMessageIdWonURIRelation(message.getMessageId(),
                                new WonURI(wonMessage.getReceiverURI(), UriType.CONNECTION));
            } catch (TelegramApiException te) {
                logger.error(te.getMessage());
            }
        }
    }
}
