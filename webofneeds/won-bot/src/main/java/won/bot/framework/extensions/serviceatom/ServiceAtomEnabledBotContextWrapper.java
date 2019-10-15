package won.bot.framework.extensions.serviceatom;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.BotContextWrapper;

import java.net.URI;

public class ServiceAtomEnabledBotContextWrapper extends BotContextWrapper implements ServiceAtomContext {
    private final String serviceAtomListName;

    public ServiceAtomEnabledBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
        serviceAtomListName = botName + ":serviceAtomUri";
    }

    @Override
    public void setServiceAtomUri(URI uri) {
        this.getBotContext().setSingleValue(serviceAtomListName, uri);
    }

    @Override
    public URI getServiceAtomUri() {
        return (URI) this.getBotContext().getSingleValue(serviceAtomListName);
    }
}
