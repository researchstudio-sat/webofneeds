package won.bot.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.core.Bot;
import won.bot.manager.BotRegistry;

import java.net.URI;
import java.util.*;

/**
 * BotRegistry, simple in-memory implementation.
 */
public class BotManagerImpl implements BotRegistry{

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private List<Bot> bots = new LinkedList<Bot>();
  private Map<URI, Bot> botByUri = new HashMap<URI, Bot>();
  private Object monitor = new Object();

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
        synchronized (getMonitor()){
          this.botByUri.put(needUri, mybot);
        }
        return mybot;
      }
    }
    return null;
  }

  @Override
  public void addBot(Bot bot) {
    synchronized (getMonitor()) {
      this.bots.add(bot);
    }
  }

  @Override
  public void setBots(Collection<Bot> bots) {
    synchronized (getMonitor()){
      this.bots.clear();
      this.bots.addAll(bots);
      this.botByUri.clear();
    }
  }

  protected Object getMonitor()
  {
    return monitor;
  }

  protected List<Bot> getBots()
  {
    return bots;
  }

  protected Map<URI, Bot> getBotByUri()
  {
    return botByUri;
  }
}
