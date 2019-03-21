package won.bot.framework.manager;

import won.bot.framework.bot.Bot;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface BotManager {
  public Bot getBotForNeedURI(URI needUri);

  public List<Bot> getBotsForNodeURI(URI nodeUri);

  public void addBot(Bot bot);

  /**
   * Drops all registered bots and uses the specified ones.
   *
   * @param bots
   */
  public void setBots(Collection<Bot> bots);

  /**
   * Indicates if all managed bots' work is done.
   */
  public boolean isWorkDone();
}
