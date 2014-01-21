package won.bot.registry.impl;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.bot.core.Bot;

import java.util.Map;

/**
 * Spring context aware bot registry that adds all beans of type Bot defined in the application context.
 */
public class SpringAwareBotRegistryImpl extends BotRegistryImpl implements InitializingBean, ApplicationContextAware {

  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Map<String, Bot> bots = this.applicationContext.getBeansOfType(Bot.class);
    this.setBots(bots.values());
  }
}
