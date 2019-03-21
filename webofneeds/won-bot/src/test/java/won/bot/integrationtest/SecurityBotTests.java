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

package won.bot.integrationtest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.SettableListenableFuture;
import won.bot.IntegrationtestBot;
import won.bot.framework.bot.base.TriggeredBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ErrorEvent;
import won.bot.framework.eventbot.event.impl.test.TestFailedEvent;
import won.bot.framework.eventbot.event.impl.test.TestFinishedEvent;
import won.bot.framework.eventbot.event.impl.test.TestPassedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.manager.BotManager;
import won.bot.integrationtest.security.DelayedDuplicateMessageSendingConversationBot;
import won.bot.integrationtest.security.DuplicateMessageSendingConversationBot;
import won.bot.integrationtest.security.DuplicateMessageURIFailureBot;
import won.bot.integrationtest.security.DuplicateNeedURIFailureBot;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Integration test.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring/app/securityBotTest.xml" })
public class SecurityBotTests {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private BotManager botManager;

  @Autowired
  ApplicationContext applicationContext;

  @Test
  public void testDuplicateNeedUri() throws Exception {
    runBot(DuplicateNeedURIFailureBot.class);
  }

  @Test
  public void testDuplicateMessageUriInCreate() throws Exception {
    runBot(DuplicateMessageURIFailureBot.class);
  }

  @Test
  public void testDuplicateMessageSendingConversationBot() throws Exception {
    runBot(DuplicateMessageSendingConversationBot.class);
  }

  @Test
  public void testDelayedDuplicateMessageSendingConversationBot() throws Exception {
    runBot(DelayedDuplicateMessageSendingConversationBot.class);
  }

  private void runBot(Class botClass) throws ExecutionException, InterruptedException {
    IntegrationtestBot bot = null;
    // create a bot instance and auto-wire it
    AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
    bot = (IntegrationtestBot) beanFactory.autowire(botClass, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
    Object botBean = beanFactory.initializeBean(bot, "theBot");
    bot = (IntegrationtestBot) botBean;
    // the bot also needs a trigger so its act() method is called regularly.
    // (there is no trigger bean in the context)
    PeriodicTrigger trigger = new PeriodicTrigger(500, TimeUnit.MILLISECONDS);
    trigger.setInitialDelay(100);
    ((TriggeredBot) bot).setTrigger(trigger);

    // adding the bot to the bot manager will cause it to be initialized.
    // at that point, the trigger starts.
    botManager.addBot(bot);

    final SettableListenableFuture<TestFinishedEvent> futureTestResult = new SettableListenableFuture();

    final EventListenerContext ctx = bot.getExposedEventListenerContext();
    // action for setting the future when we get a test result
    EventBotAction setFutureAction = new SetFutureAction(ctx, futureTestResult);
    // action for setting the future when an error occurs
    EventBotAction setFutureFromErrorAction = new SetFutureFromErrorEventAction(ctx, futureTestResult, bot);
    // add a listener for test success
    ctx.getEventBus().subscribe(TestPassedEvent.class, new ActionOnEventListener(ctx, setFutureAction));
    // add a listener for test failure
    ctx.getEventBus().subscribe(TestFailedEvent.class, new ActionOnEventListener(ctx, setFutureAction));
    // add a listener for errors
    ctx.getEventBus().subscribe(ErrorEvent.class, new ActionOnEventListener(ctx, setFutureFromErrorAction));

    // wait for the result
    TestFinishedEvent result = futureTestResult.get();
    if (result instanceof TestFailedEvent) {
      Assert.fail(((TestFailedEvent) result).getMessage());
    }
  }

  private class SetFutureAction extends BaseEventBotAction {
    private SettableListenableFuture<TestFinishedEvent> futureTestResult;

    private SetFutureAction(EventListenerContext eventListenerContext,
        SettableListenableFuture<TestFinishedEvent> futureTestResult) {
      super(eventListenerContext);
      this.futureTestResult = futureTestResult;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
      if (event instanceof TestFinishedEvent) {
        futureTestResult.set((TestFinishedEvent) event);
      } else {
        logger.warn("cannot handle event {}", event);
      }
    }

  }

  private class SetFutureFromErrorEventAction extends BaseEventBotAction {
    private SettableListenableFuture<TestFinishedEvent> futureTestResult;
    private IntegrationtestBot bot;

    private SetFutureFromErrorEventAction(EventListenerContext eventListenerContext,
        SettableListenableFuture<TestFinishedEvent> futureTestResult, IntegrationtestBot bot) {
      super(eventListenerContext);
      this.futureTestResult = futureTestResult;
      this.bot = bot;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
      if (event instanceof ErrorEvent) {

        StringBuilder message = new StringBuilder();
        getMessageFromCauses(message, ((ErrorEvent) event).getThrowable());
        futureTestResult.set(new TestFailedEvent(bot, message.toString()));
      } else {
        logger.warn("cannot handle event {}", event);
      }
    }
  }

  private void getMessageFromCauses(StringBuilder stringBuilder, Throwable throwable) {
    if (throwable == null)
      return;
    stringBuilder.append(throwable.getMessage());
    Throwable cause = throwable.getCause();
    if (cause != null) {
      stringBuilder.append(" -- Cause: ");
      getMessageFromCauses(stringBuilder, cause);
    }
  }
}
