package won.bot.framework.manager.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.bot.Bot;
import won.bot.framework.manager.BotManager;

/**
 * BotManager, simple in-memory implementation.
 */
public class BotManagerImpl implements BotManager
{

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private List<Bot> bots = new LinkedList<Bot>();
  private Map<URI, Bot> botByUri = new HashMap<URI, Bot>();
  private Map<URI, List<Bot>> botListByUri = new HashMap<URI, List<Bot>>();
  private Object monitor = new Object();

  @Override
  public Bot getBotForNeedURI(URI needUri) {
    //try the botByUri map
    {
      Bot bot = botByUri.get(needUri);
      if (bot != null) return bot;
    }
    //check each bot, return first that knows the needUri
    logger.debug("bots size:{} ", bots.size());
    for(Bot mybot: bots){
      //logger.debug("bot knows need: {}", mybot.knowsNeedURI(needUri));
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
  public List<Bot> getBotsForNodeURI(final URI wonNodeUri) {
    {
      List<Bot> botList = botListByUri.get(wonNodeUri);
      if (botList!=null && botList.size()>0) return botList;
    }
    List<Bot> botList = new ArrayList<Bot>();
    for (Bot mybot:bots){
      if (mybot.knowsNodeURI(wonNodeUri)){
        synchronized (getMonitor()){
          botList.add(mybot);
        }
      }
    }
    this.botListByUri.put(wonNodeUri,botList);
    return botList;
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
    logger.debug("checking if the bots' work is all done");
    synchronized (getMonitor()){
      for(Bot bot: getBots()){
        if (! bot.isWorkDone()) {
          logger.debug("bot {} is not done yet", bot);
          return false;
        }
      }
    }
    logger.debug("all bots are done");
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
