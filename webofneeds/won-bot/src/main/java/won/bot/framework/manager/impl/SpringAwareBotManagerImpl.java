package won.bot.framework.manager.impl;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import won.bot.framework.bot.Bot;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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


  @Override
  public void onApplicationEvent(final ApplicationEvent event)
  {
    if (event instanceof ContextStartedEvent){
      try {
        findAndRegisterBots();
      } catch (Exception e) {
        logger.warn("Error registering bots", e);
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
        if (isWorkDone()) SpringApplication.exit(applicationContext);
      }
    }, checkWorkDoneTrigger);
  }

  private void findBotsInContextAndInitialize()
  {
    Map<String, Bot> bots = this.applicationContext.getBeansOfType(Bot.class);
    this.setBots(bots.values());
    for (Bot bot: bots.values()) {
      try {
        initializeBotIfNecessary(bot);
      } catch (Exception e){
        logger.warn("could not initialize bot {}", bot, e);
      }
    }
  }

}
