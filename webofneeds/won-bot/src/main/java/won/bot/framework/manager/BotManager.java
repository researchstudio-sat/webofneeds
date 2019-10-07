package won.bot.framework.manager;

import won.bot.framework.bot.Bot;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface BotManager {
    Bot getBotResponsibleForAtomUri(URI atomUri);

    List<Bot> getBotsForNodeURI(URI nodeUri);

    void addBot(Bot bot);

    /**
     * Drops all registered bots and uses the specified ones.
     * 
     * @param bots
     */
    void setBots(Collection<Bot> bots);

    /**
     * Indicates if all managed bots' work is done.
     */
    boolean isWorkDone();
}
