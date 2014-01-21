package won.bot.registry.impl;

import won.bot.core.Bot;
import won.bot.registry.BotRegistry;

import java.net.URI;
import java.util.*;

/**
 * BotRegistry, simple in-memory implementation.
 */
public class BotRegistryImpl implements BotRegistry{

  private List<Bot> bots = new LinkedList<Bot>();
  private Map<URI, Bot> botByUri = new HashMap<URI, Bot>();

  @Override
  public Bot getBot(URI needUri) {
    //try the botByUri map
    {
      Bot bot = botByUri.get(needUri);
      if (bot != null) return bot;
    }
    //check each bot, return first that knows the needUri
    for(Bot mybot: bots){
      if (mybot.knowsNeedURI(needUri)) {
        synchronized (this){
          this.botByUri.put(needUri, mybot);
        }
        return mybot;
      }
    }
    return null;
  }

  @Override
  public synchronized void addBot(Bot bot) {
    this.bots.add(bot);
  }

  @Override
  public synchronized void setBots(Collection<Bot> bots) {
    this.bots.clear();
    this.bots.addAll(bots);
    this.botByUri.clear();
  }
}
