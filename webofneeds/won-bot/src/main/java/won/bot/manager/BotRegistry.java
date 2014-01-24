package won.bot.manager;

import won.bot.core.Bot;

import java.net.URI;
import java.util.Collection;

/**
 *
 */
public interface BotRegistry {
  public Bot getBot(URI needUri);
  public void addBot(Bot bot);

  /**
   * Drops all registered bots and uses the specified ones.
   * @param bots
   */
  public void setBots(Collection<Bot> bots);
}
