package won.bot.framework.bot.context;

import java.net.URI;

public class BotServiceAtomEnabledBotContextWrapper extends BotContextWrapper implements BotServiceAtomContext {
    private final String serviceAtomListName;

    public BotServiceAtomEnabledBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
        serviceAtomListName = botName + ":serviceAtomUri";
    }

    @Override
    public void setBotServiceAtomUri(URI uri) {
        this.getBotContext().setSingleValue(serviceAtomListName, uri);
    }

    @Override
    public URI getBotServiceAtomUri() {
        return (URI) this.getBotContext().getSingleValue(serviceAtomListName);
    }
}
