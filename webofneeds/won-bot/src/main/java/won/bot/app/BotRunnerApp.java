/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.app;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.support.PeriodicTrigger;
import won.bot.framework.bot.Bot;
import won.bot.framework.bot.base.TriggeredBot;
import won.bot.framework.manager.BotManager;

import java.util.concurrent.TimeUnit;

/**
 * Runs any bot by class name.
 */
public class BotRunnerApp {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("arguments: [bot class name]");
      System.exit(1);
    }
    String botClass = args[0];

    SpringApplication app = new SpringApplication(new Object[] { "classpath:/spring/app/botRunner.xml" });
    app.setWebEnvironment(false);
    ConfigurableApplicationContext applicationContext = app.run(args);

    Bot bot = null;
    // create a bot instance and auto-wire it
    AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
    bot = (Bot) beanFactory.autowire(Class.forName(botClass), AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
    Object botBean = beanFactory.initializeBean(bot, "theBot");
    bot = (Bot) botBean;
    // the bot also needs a trigger so its act() method is called regularly.
    // (there is no trigger bean in the context)
    if (bot instanceof TriggeredBot) {
      PeriodicTrigger trigger = new PeriodicTrigger(5000, TimeUnit.MILLISECONDS);
      trigger.setInitialDelay(1000);
      ((TriggeredBot) bot).setTrigger(trigger);
    }

    BotManager botManager = (BotManager) applicationContext.getBean("botManager");
    // adding the bot to the bot manager will cause it to be initialized.
    // at that point, the trigger starts.
    botManager.addBot(bot);
  }

}
