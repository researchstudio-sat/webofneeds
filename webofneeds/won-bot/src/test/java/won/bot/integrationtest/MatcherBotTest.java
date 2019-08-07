/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.integrationtest;

import org.junit.Assert;
import org.junit.Before;
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
import won.bot.framework.eventbot.event.impl.lifecycle.WorkDoneEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.MatcherProtocolTestBot;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * Integration test.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring/app/simpleMatcherTest.xml" })
public class MatcherBotTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int RUN_ONCE = 1;
    private static final long ACT_LOOP_TIMEOUT_MILLIS = 1000;
    private static final long ACT_LOOP_INITIAL_DELAY_MILLIS = 2000;
    MyBot bot;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    SpringAwareBotManagerImpl botManager;

    /**
     * This is run before each @TestD method.
     */
    @Before
    public void before() {
        // create a bot instance and auto-wire it
        this.bot = (MyBot) applicationContext.getAutowireCapableBeanFactory().autowire(MyBot.class,
                        AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
        // the bot also atoms a trigger so its act() method is called regularly.
        // (there is no trigger bean in the context)
        PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
        this.bot.setTrigger(trigger);
    }

    /**
     * The main test method.
     * 
     * @throws Exception
     */
    @Test
    public void testMatcherBot() throws Exception {
        logger.info("starting test case testMatcherBot");
        // adding the bot to the bot manager will cause it to be initialized.
        // at that point, the trigger starts.
        botManager.addBot(this.bot);
        // the bot should now be running. We have to wait for it to finish before we
        // can check the results:
        // Together with the barrier.await() in the bot's listener, this trips the
        // barrier
        // and both threads continue.
        this.bot.getBarrier().await();
        // now check the results!
        this.bot.executeAsserts();
        logger.info("finishing test case testCommentBot");
    }

    /**
     * We create a subclass of the bot we want to test here so that we can add a
     * listener to its internal event bus and to access its listeners, which record
     * information during the run that we later check with asserts.
     */
    public static class MyBot extends MatcherProtocolTestBot {
        /**
         * Used for synchronization with the @TestD method: it should wait at the
         * barrier until our bot is done, then execute the asserts.
         */
        CyclicBarrier barrier = new CyclicBarrier(2);

        /**
         * Default constructor is required for instantiation through Spring.
         */
        public MyBot() {
        }

        @Override
        protected void initializeEventListeners() {
            // of course, let the real bot implementation initialize itself
            super.initializeEventListeners();
            // now, add a listener to the WorkDoneEvent.
            // its only purpose is to trip the CyclicBarrier instance that
            // the test method is waiting on
            getEventBus().subscribe(WorkDoneEvent.class, new ActionOnEventListener(getEventListenerContext(),
                            new TripBarrierAction(getEventListenerContext(), barrier)));
        }

        public CyclicBarrier getBarrier() {
            return barrier;
        }

        /**
         * Here we check the results of the bot's execution.
         */
        public void executeAsserts() {
            Assert.assertEquals(1, this.matcherRegistrator.getEventCount());
            Assert.assertEquals(0, this.matcherRegistrator.getExceptionCount());
            // 1 act events
            Assert.assertEquals(2, this.atomCreator.getEventCount());
            Assert.assertEquals(0, this.atomCreator.getExceptionCount());
            Assert.assertEquals(4, this.matcherNotifier.getEventCount());
            Assert.assertEquals(0, this.matcherNotifier.getExceptionCount());
            // 1 create atom events
            Assert.assertEquals(2, this.matcher.getEventCount());
            Assert.assertEquals(0, this.matcher.getExceptionCount());
            Assert.assertEquals(1, this.allAtomsDeactivator.getEventCount());
            Assert.assertEquals(0, this.allAtomsDeactivator.getExceptionCount());
            // 4 AtomDeactivated events
            Assert.assertEquals(4, this.workDoneSignaller.getEventCount());
            Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());
            // TODO: there is more to check:
            // * what does the RDF look like?
            // --> pull it from the atomURI/ConnectionURI and check contents
            // * what does the database look like? */
        }
    }
}
