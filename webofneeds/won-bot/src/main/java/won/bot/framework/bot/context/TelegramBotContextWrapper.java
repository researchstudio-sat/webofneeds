package won.bot.framework.bot.context;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;

import java.net.URI;

/**
 * Created by fsuda on 14.04.2017.
 */
public class TelegramBotContextWrapper extends BotContextWrapper {
    private String chatIdUriRelationsName = "tgChatIdUriRelations";
    private String messageIdUriRelationsName = "tgMessageIdUriRelations";
    private String uriChatIdRelationsName = "tgUriChatIdRelations";

    public TelegramBotContextWrapper(BotContext botContext, String needCreateListName, String chatIdUriRelationsName, String messageIdUriRelationsName, String uriChatIdRelationsName) {
        super(botContext, needCreateListName);
        this.chatIdUriRelationsName = chatIdUriRelationsName;
        this.messageIdUriRelationsName = messageIdUriRelationsName;
        this.uriChatIdRelationsName = uriChatIdRelationsName;
    }

    public String getChatIdUriRelationsName() {
        return chatIdUriRelationsName;
    }

    public String getMessageIdUriRelationsName() {
        return messageIdUriRelationsName;
    }

    public String getUriChatIdRelationsName() {
        return uriChatIdRelationsName;
    }

    public void addChatIdWonURIRelation(Long chatId, WonURI uri) {
        getBotContext().saveToObjectMap(chatIdUriRelationsName, chatId.toString(), uri);
    }

    public void addURIChatIdRelation(URI uri, Long chatId) {
        getBotContext().saveToObjectMap(uriChatIdRelationsName, uri.toString(), chatId);
    }

    public Long getChatIdForURI(URI uri) {
        return (Long) getBotContext().loadFromObjectMap(uriChatIdRelationsName, uri.toString());
    }

    public void addMessageIdWonURIRelation(Integer messageId, WonURI wonURI) {
        getBotContext().saveToObjectMap(messageIdUriRelationsName, messageId.toString(), wonURI);
    }

    public WonURI getWonURIForMessageId(Integer messageId) {
        return (WonURI) getBotContext().loadFromObjectMap(messageIdUriRelationsName, messageId.toString());
    }
}
