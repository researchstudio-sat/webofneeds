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
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Connect2TelegramAction extends BaseEventBotAction {
    WonTelegramBotHandler wonTelegramBotHandler;

    public Connect2TelegramAction(EventListenerContext ctx, WonTelegramBotHandler wonTelegramBotHandler) {
        super(ctx);
        this.wonTelegramBotHandler = wonTelegramBotHandler;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof ConnectFromOtherAtomEvent
                        && ctx.getBotContextWrapper() instanceof TelegramBotContextWrapper) {
            TelegramBotContextWrapper botContextWrapper = (TelegramBotContextWrapper) ctx.getBotContextWrapper();
            Connection con = ((ConnectFromOtherAtomEvent) event).getCon();
            URI yourAtomUri = con.getAtomURI();
            URI targetAtomUri = con.getTargetAtomURI();
            Long chatId = botContextWrapper.getChatIdForURI(yourAtomUri);
            if (chatId == null) {
                logger.error("No chatId found for the specified atomUri");
                return;
            }
            try {
                Message message = wonTelegramBotHandler.sendMessage(wonTelegramBotHandler.getTelegramMessageGenerator()
                                .getConnectMessage(chatId, targetAtomUri, yourAtomUri));
                botContextWrapper.addMessageIdWonURIRelation(message.getMessageId(),
                                new WonURI(con.getConnectionURI(), UriType.CONNECTION));
            } catch (TelegramApiException te) {
                logger.error(te.getMessage());
            }
        }
    }
}
