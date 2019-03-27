package won.bot.framework.bot.context;

import java.net.URI;

/**
 * Created by MS on 24.09.2018.
 */
public class HokifyJobBotContextWrapper extends BotContextWrapper {
    private String uriJobURLRelationsName = getBotName() + ":uriJobURLRelations";
    private String jobUrlUriRelationsName = getBotName() + ":jobUrlUriRelations";

    public HokifyJobBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }

    public void addURIJobURLRelation(String jobURL, URI uri) {
        getBotContext().saveToObjectMap(jobUrlUriRelationsName, jobURL, uri.toString());
        getBotContext().saveToObjectMap(uriJobURLRelationsName, uri.toString(), jobURL);
    }

    public void removeURIJobURLRelation(URI uri) {
        String jobURL = (String) getBotContext().loadFromObjectMap(uriJobURLRelationsName, uri.toString());
        getBotContext().removeFromObjectMap(uriJobURLRelationsName, uri.toString());
        getBotContext().removeFromObjectMap(jobUrlUriRelationsName, jobURL);
    }

    public String getJobURLForURI(URI uri) {
        return (String) getBotContext().loadFromObjectMap(uriJobURLRelationsName, uri.toString());
    }

    public String getNeedUriForJobURL(String jobURL) {
        return (String) getBotContext().loadFromObjectMap(jobUrlUriRelationsName, jobURL);
    }
}
