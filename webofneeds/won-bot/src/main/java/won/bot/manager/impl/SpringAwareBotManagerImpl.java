package won.bot.manager.impl;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.bot.core.Bot;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Spring context aware bot registry that adds all beans of type Bot defined in the application context.
 */
public class SpringAwareBotManagerImpl extends BotManagerImpl implements InitializingBean, ApplicationContextAware, DisposableBean
{

  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    logger.info("starting up bot manager");
    synchronized (getMonitor()) {
      Map<String, Bot> bots = this.applicationContext.getBeansOfType(Bot.class);
      this.setBots(bots.values());
      for (Bot bot: bots.values()) {
        try {
          if (bot.getLifecyclePhase().isDown()) {
            logger.info("initializing bot {}", bot);
            bot.initialize();
          }
        } catch (Exception e){
          logger.warn("could not initialize bot {}", bot, e);
        }
      }
    }
    logger.info("bot manager startup complete");
  }

  @Override
  public void destroy() throws Exception
  {
    logger.info("shutting down bot manager");
    synchronized (getMonitor()){
      List<Bot> bots = getBots();
      Bot bot;
      for(Iterator<Bot> it = bots.iterator(); it.hasNext(); ){
        bot = it.next();
        if(bot.getLifecyclePhase().isActive()){
          try {
            bot.shutdown();
          } catch (Exception e) {
            logger.warn("could not shut down bot {}", bot, e);
          }
        }
        it.remove();
      }
    }
    logger.info("bot manager shutdown complete");
  }
}
