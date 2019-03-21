package won.bot.framework.bot.context;

import won.bot.framework.eventbot.action.impl.mail.model.WonURI;

import java.net.URI;

/**
 * Created by fsuda on 14.04.2017.
 */
public class TelegramBotContextWrapper extends BotContextWrapper {
  private String chatIdUriRelationsName = getBotName() + ":chatIdUriRelations";
  private String messageIdUriRelationsName = getBotName() + ":messageIdUriRelations";
  private String uriChatIdRelationsName = getBotName() + ":uriChatIdRelations";

  public TelegramBotContextWrapper(BotContext botContext, String botName) {
    super(botContext, botName);
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
