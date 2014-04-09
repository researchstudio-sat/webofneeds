package won.bot.framework.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.bot.Bot;
import won.bot.framework.manager.BotManager;

import java.net.URI;
import java.util.*;

/**
 * BotManager, simple in-memory implementation.
 */
public class BotManagerImpl implements BotManager
{

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
      if (bots.contains(bot)) return;
      initializeBotIfNecessary(bot);
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

  @Override
  public boolean isWorkDone()
  {
    synchronized (getMonitor()){
      for(Bot bot: getBots()){
        if (! bot.isWorkDone()) return false;
      }
    }
    return true;
  }

  protected void initializeBotIfNecessary(Bot bot){
    if (bot.getLifecyclePhase().isDown()){
      try {
        logger.info("initializing bot {}", bot);
        bot.initialize();
      } catch (Exception e) {
        logger.warn("could not initialize bot {} ",bot, e);
      }
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
