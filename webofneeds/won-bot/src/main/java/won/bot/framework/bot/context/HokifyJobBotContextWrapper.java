package won.bot.framework.bot.context;

import java.net.URI;

import won.bot.framework.eventbot.action.impl.mail.model.WonURI;

/**
 * Created by MS on 24.09.2018.
 */
public class HokifyJobBotContextWrapper extends BotContextWrapper {

    private String uriJobURLRelationsName = getBotName() + ":uriJobURLRelationsName";

    public HokifyJobBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }
    
    public void addURIJobURLRelation(String jobURL, URI uri) {
        getBotContext().saveToObjectMap(uriJobURLRelationsName, jobURL,  uri.toString());
        getBotContext().saveToObjectMap(uriJobURLRelationsName, uri.toString(), jobURL);
    }
    
    public void removeURIJobURLRelation(URI uri) {
        String jobURL = (String) getBotContext().loadFromObjectMap(uriJobURLRelationsName, uri.toString());
        getBotContext().removeFromObjectMap(uriJobURLRelationsName, uri.toString());
        getBotContext().removeFromObjectMap(uriJobURLRelationsName, jobURL);
    }
    
    
    public String getJobURLForURI(URI uri) {
        return (String) getBotContext().loadFromObjectMap(uriJobURLRelationsName, uri.toString());
    }
    
    public String getNeedUriForJobURL(String jobURL) {
        return (String) getBotContext().loadFromObjectMap(uriJobURLRelationsName, jobURL);
    }
    
    
}
