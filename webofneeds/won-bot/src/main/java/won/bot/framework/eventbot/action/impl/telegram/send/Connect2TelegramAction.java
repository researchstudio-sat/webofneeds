package won.bot.framework.eventbot.action.impl.telegram.send;

import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;

import java.net.URI;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Connect2TelegramAction extends BaseEventBotAction {
    private String uriChatIdRelationsName;
    private String messageIdUriListName;

    WonTelegramBotHandler wonTelegramBotHandler;

    public Connect2TelegramAction(EventListenerContext ctx, WonTelegramBotHandler wonTelegramBotHandler, String uriChatIdRelationsName, String messageIdUriListName) {
        super(ctx);
        this.uriChatIdRelationsName = uriChatIdRelationsName;
        this.wonTelegramBotHandler = wonTelegramBotHandler;
        this.messageIdUriListName = messageIdUriListName;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (event instanceof ConnectFromOtherNeedEvent) {
            Connection con = ((ConnectFromOtherNeedEvent) event).getCon();

            URI yourNeedUri = con.getNeedURI();
            URI remoteNeedUri = con.getRemoteNeedURI();

            Long chatId = EventBotActionUtils.getChatIdForURI(getEventListenerContext(), uriChatIdRelationsName, yourNeedUri);
            if(chatId == null) {
                logger.error("No chatId found for the specified needUri");
                return;
            }

            try{
                Message message = wonTelegramBotHandler.sendMessage(wonTelegramBotHandler.getTelegramMessageGenerator().getConnectMessage(chatId, remoteNeedUri, yourNeedUri));
                EventBotActionUtils.addMessageIdWonURIRelation(getEventListenerContext(), messageIdUriListName, message.getMessageId(), new WonURI(con.getConnectionURI(), UriType.CONNECTION));
            }catch (TelegramApiException te){
                logger.error(te.getMessage());
            }
        }
    }
}
