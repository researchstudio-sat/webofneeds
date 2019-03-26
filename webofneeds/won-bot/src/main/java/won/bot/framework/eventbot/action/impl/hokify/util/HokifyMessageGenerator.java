package won.bot.framework.eventbot.action.impl.hokify.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import won.bot.framework.eventbot.EventListenerContext;
import won.protocol.message.WonMessage;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.WonRdfUtils;

/**
 * Created by MS on 17.09.2018.
 */
public class HokifyMessageGenerator {
  private static final Logger logger = LoggerFactory.getLogger(HokifyMessageGenerator.class);
  private EventListenerContext eventListenerContext;

  public SendMessage getHintMessage(URI remoteNeedUri, URI yourNeedUri) {
    Dataset remoteNeedRDF = eventListenerContext.getLinkedDataSource().getDataForResource(remoteNeedUri);

    DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(remoteNeedRDF);
    String title = needModelWrapper.getSomeTitleFromIsOrAll("en", "de");
    String description = needModelWrapper.getSomeDescription("en", "de");

    SendMessage sendMessage = new SendMessage();
    // sendMessage.setJobURL(jobURL);
    String text = "<b>We found a Match for you!\n\n</b><a href='" + remoteNeedUri + "'>" + title + "\n\n</a>";

    if (description != null) {
      text = text + "<em>" + description + "</em>";
    }
    sendMessage.setText(text);
    sendMessage.setReplyMarkup(getConnectionActionKeyboard("Request", "Close"));
    sendMessage.enableHtml(true);

    return sendMessage;
  }

  public SendMessage getConnectMessage(URI remoteNeedUri, URI yourNeedUri) {
    Dataset remoteNeedRDF = eventListenerContext.getLinkedDataSource().getDataForResource(remoteNeedUri);

    DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(remoteNeedRDF);
    String title = needModelWrapper.getSomeTitleFromIsOrAll("en", "de");
    String description = needModelWrapper.getSomeDescription("en", "de");

    SendMessage sendMessage = new SendMessage();
    // sendMessage.setChatId(chatId);

    String text = "<b>Someone wants to connect with you!\n\n</b><a href='" + remoteNeedUri + "'>" + title + "\n\n</a>";

    if (description != null) {
      text = text + "<em>" + description + "</em>";
    }
    sendMessage.setText(text);
    sendMessage.setReplyMarkup(getConnectionActionKeyboard("Accept", "Deny"));
    sendMessage.enableHtml(true);

    return sendMessage;
  }

  public SendMessage getConnectionTextMessage(URI remoteNeedUri, URI yourNeedUri, WonMessage message) {
    SendMessage sendMessage = new SendMessage();
    // sendMessage.setChatId(chatId);

    sendMessage.setText("<a href='" + remoteNeedUri + "'>URI</a>: " + extractTextMessageFromWonMessage(message));
    sendMessage.enableHtml(true);

    return sendMessage;
  }

  public SendMessage getCreatedNeedMessage(Long chatId, URI needURI) {
    Dataset createdNeedRDF = eventListenerContext.getLinkedDataSource().getDataForResource(needURI);

    DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(createdNeedRDF);
    String title = needModelWrapper.getSomeTitleFromIsOrAll("en", "de");
    String description = needModelWrapper.getSomeDescription("en", "de");

    SendMessage sendMessage = new SendMessage();
    sendMessage.setChatId(chatId);
    String text = "<b>We created a Need for you!\n\n</b><a href='" + needURI + "'>" + title + "\n\n</a>";

    if (description != null) {
      text = text + "<em>" + description + "</em>";
    }
    sendMessage.setText(text);
    sendMessage.enableHtml(true);

    return sendMessage;
  }

  public SendMessage getErrorMessage(Long chatId) {
    SendMessage sendMessage = new SendMessage();
    sendMessage.setChatId(chatId);
    sendMessage.setText("could not create need wrong syntax");

    return sendMessage;
  }

  private static String extractTextMessageFromWonMessage(WonMessage wonMessage) {
    if (wonMessage == null)
      return null;
    String message = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
    return StringUtils.trim(message);
  }

  private InlineKeyboardMarkup getConnectionActionKeyboard(String acceptText, String denyText) {
    InlineKeyboardMarkup connectionActionKeyboard = new InlineKeyboardMarkup();

    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
    List<InlineKeyboardButton> row = new ArrayList<>();
    InlineKeyboardButton denyButton = new InlineKeyboardButton();
    denyButton.setText(denyText);
    denyButton.setCallbackData("0");
    InlineKeyboardButton acceptButton = new InlineKeyboardButton();
    acceptButton.setText(acceptText);
    acceptButton.setCallbackData("1");

    row.add(acceptButton);
    row.add(denyButton);
    rows.add(row);
    connectionActionKeyboard.setKeyboard(rows);

    return connectionActionKeyboard;
  }

  public void setEventListenerContext(EventListenerContext eventListenerContext) {
    this.eventListenerContext = eventListenerContext;
  }
}
