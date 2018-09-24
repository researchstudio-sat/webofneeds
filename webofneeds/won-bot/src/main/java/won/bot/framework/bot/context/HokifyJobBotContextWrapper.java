package won.bot.framework.bot.context;

import java.net.URI;

import won.bot.framework.eventbot.action.impl.mail.model.WonURI;

/**
 * Created by MS on 24.09.2018.
 */
public class HokifyJobBotContextWrapper extends BotContextWrapper {
    private String chatIdUriRelationsName = getBotName() + ":chatIdUriRelations";
    private String messageIdUriRelationsName = getBotName() + ":messageIdUriRelations";
    private String uriJobURLRelationsName = getBotName() + ":uriJobURLRelationsName";

    public HokifyJobBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }
    
    public void addURIJobURLRelation(URI uri, String jobURL) {
        getBotContext().saveToObjectMap(uriJobURLRelationsName, uri.toString(), jobURL);
    }
    
    public void removeURIJobURLRelation(URI uri) {
        getBotContext().removeFromObjectMap(uriJobURLRelationsName, uri.toString());
    }
    
    

    public void addChatIdWonURIRelation(Long chatId, WonURI uri) {
        getBotContext().saveToObjectMap(chatIdUriRelationsName, chatId.toString(), uri);
    }

    

    public String getJobURLForURI(URI uri) {
        return (String) getBotContext().loadFromObjectMap(uriJobURLRelationsName, uri.toString());
    }

    public void addMessageIdWonURIRelation(Integer messageId, WonURI wonURI) {
        getBotContext().saveToObjectMap(messageIdUriRelationsName, messageId.toString(), wonURI);
    }

    public WonURI getWonURIForMessageId(Integer messageId) {
        return (WonURI) getBotContext().loadFromObjectMap(messageIdUriRelationsName, messageId.toString());
    }
}
