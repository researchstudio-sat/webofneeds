package won.bot.framework.manager.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import won.bot.framework.bot.Bot;

/**
 * Spring context aware bot registry that adds all beans of type Bot defined in the application context.
 *
 * If the checkWorkDoneTrigger is not null, all bots will regularly be checked if all work is done. If so, the
 * spring context will be shut down.
 */
public class SpringAwareBotManagerImpl extends BotManagerImpl implements ApplicationContextAware, DisposableBean, ApplicationListener
{

  private ApplicationContext applicationContext;
  private Trigger checkWorkDoneTrigger = null;
  @Autowired
  private TaskScheduler taskScheduler;

  private boolean shutdownApplicationContextIfWorkDone = false;


  @Override
  public void onApplicationEvent(final ApplicationEvent event)
  {
    logger.debug("processing application event {}", event);
    if (event instanceof ContextRefreshedEvent){
      logger.info("context started or refreshed: searching for bots in spring context");
      try {
        findAndRegisterBots();
      } catch (Exception e) {
        logger.warn("Error registering bots", e);
      }
    } else if (event instanceof ContextClosedEvent) {
      try {
        destroy();
      } catch (Exception e) {
        logger.warn("Error destroying bot manager "+this, e);
      }
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
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

  private void findAndRegisterBots() throws Exception {
    logger.info("starting up bot manager");
    synchronized (getMonitor()) {
      findBotsInContextAndInitialize();
    }
    registerCheckWorkDoneTrigger();
    logger.info("bot manager startup complete");
  }

  public void setCheckWorkDoneTrigger(final Trigger checkWorkDoneTrigger)
  {
    this.checkWorkDoneTrigger = checkWorkDoneTrigger;
  }

  public void setTaskScheduler(final TaskScheduler taskScheduler)
  {
    this.taskScheduler = taskScheduler;
  }

  public boolean isShutdownApplicationContextIfWorkDone() {
    return shutdownApplicationContextIfWorkDone;
  }

  public void setShutdownApplicationContextIfWorkDone(final boolean shutdownApplicationContextIfWorkDone) {
    this.shutdownApplicationContextIfWorkDone = shutdownApplicationContextIfWorkDone;
  }

  private void registerCheckWorkDoneTrigger()
  {
    if (this.checkWorkDoneTrigger == null){
      logger.info("no trigger set on SpringAwareBotManagerImpl, not checking bots' workDone status");
      return;
    }
    this.taskScheduler.schedule(new Runnable(){
      @Override
      public void run()
      {
        boolean workDone = isWorkDone();
        if (! shutdownApplicationContextIfWorkDone){
          logger.debug("botmanager will not shutdown spring context when work is done. (workDone:{})",workDone );
        } else {
          logger.debug("botmanager will shutdown spring context when work is done. (workDone:{})",workDone );
          if (workDone) SpringApplication.exit(applicationContext);
        }
      }
    }, checkWorkDoneTrigger);
  }

  private void findBotsInContextAndInitialize()
  {
    Map<String, Bot> bots = this.applicationContext.getBeansOfType(Bot.class);
    for (Bot bot: bots.values()) {
      try {
        addBot(bot);
      } catch (Exception e){
        logger.warn("could not initialize bot {}", bot, e);
      }
    }
  }

}
