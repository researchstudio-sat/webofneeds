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
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.CountingListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.GroupingBot;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * Integration test.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring/app/simple2AtomGroupingTest.xml" })
public class GroupingBotTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // private static final int RUN_ONCE = 1;
    private static final long ACT_LOOP_TIMEOUT_MILLIS = 100;
    private static final long ACT_LOOP_INITIAL_DELAY_MILLIS = 100;
    private static boolean run = false;
    private static MyBot bot;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    SpringAwareBotManagerImpl botManager;

    /**
     * This is run before each @TestD method.
     */
    @Before
    public void before() {
        if (!run) {
            AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
            bot = (MyBot) beanFactory.autowire(MyBot.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
            Object botBean = beanFactory.initializeBean(bot, "mybot");
            bot = (MyBot) botBean;
            // the bot also atoms a trigger so its act() method is called regularly.
            // (there is no trigger bean in the context)
            PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
            bot.setTrigger(trigger);
            logger.info("starting test case testGroupBot");
            // adding the bot to the bot manager will cause it to be initialized.
            // at that point, the trigger starts.
            botManager.addBot(bot);
            // the bot should now be running. We have to wait for it to finish before we
            // can check the results:
            // Together with the barrier.await() in the bot's listener, this trips the
            // barrier
            // and both threads continue.
            try {
                bot.getBarrier().await();
            } catch (InterruptedException e) {
                e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
            } catch (BrokenBarrierException e) {
                e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
            }
            run = true;
        }
    }

    /**
     * The main test method.
     * 
     * @throws Exception
     */
    @Test
    public void testGroupingBot() throws Exception {
        logger.info("starting test case testCreate2AtomsGroupingBot");
        this.bot.executeAsserts();
        logger.info("finishing test case testCreate2AtomsGroupingBot");
    }

    /*
     * @Test public void testGroupRDFBot() throws Exception {
     * logger.info("starting test case testCreate2AtomsGroupingBot");
     * this.bot.executeGroupRDFValidationAssert();
     * logger.info("finishing test case testCreate2AtomsGroupingBot"); }
     */
    /**
     * We create a subclass of the bot we want to test here so that we can add a
     * listener to its internal event bus and to access its listeners, which record
     * information during the run that we later check with asserts.
     */
    public static class MyBot extends GroupingBot {
        /**
         * Used for synchronization with the @TestD method: it should wait at the
         * barrier until our bot is done, then execute the asserts.
         */
        CyclicBarrier barrier = new CyclicBarrier(2);
        // private static final String sparqlPrefix = "PREFIX rdfs:
        // <http://www.w3.org/2000/01/rdf-schema#>"
        // + "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>"
        // + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
        // + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
        // + "PREFIX won: <https://w3id.org/won/core#>" + "PREFIX gr:
        // <http://purl.org/goodrelations/v1#>"
        // + "PREFIX ldp: <http://www.w3.org/ns/ldp#>";

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
            // 2 act events
            Assert.assertEquals(NO_OF_GROUPMEMBERS, this.groupMemberCreator.getEventCount());
            Assert.assertEquals(0, this.groupMemberCreator.getExceptionCount());
            // 2 create atom events
            Assert.assertEquals(NO_OF_GROUPMEMBERS, this.groupCreator.getEventCount());
            Assert.assertEquals(0, this.groupCreator.getExceptionCount());
            // 1 create group events
            Assert.assertEquals(NO_OF_GROUPMEMBERS + 1, this.atomConnector.getEventCount());
            Assert.assertEquals(0, this.atomConnector.getExceptionCount());
            // 2 connect, 2 open
            Assert.assertEquals(NO_OF_GROUPMEMBERS, this.autoOpener.getEventCount());
            Assert.assertEquals(0, this.autoOpener.getExceptionCount());
            // 41 messages
            Assert.assertEquals(NO_OF_GROUPMEMBERS * 2, this.messagesDoneListener.getEventCount());
            Assert.assertEquals(0, this.messagesDoneListener.getExceptionCount());
            // check that the autoresponder creator was called
            Assert.assertEquals(NO_OF_GROUPMEMBERS, this.autoResponderCreator.getEventCount());
            Assert.assertEquals(0, this.autoResponderCreator.getExceptionCount());
            // check that the autoresponders were created
            Assert.assertEquals(NO_OF_GROUPMEMBERS, this.autoResponders.size());
            // check that the autoresponders responded as they should
            for (BaseEventListener autoResponder : this.autoResponders) {
                Assert.assertEquals(NO_OF_MESSAGES, ((CountingListener) autoResponder).getCount());
                Assert.assertEquals(0, autoResponder.getExceptionCount());
            }
            // 4 AtomDeactivated events
            Assert.assertEquals(NO_OF_GROUPMEMBERS, this.workDoneSignaller.getEventCount());
            Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());
            // TODO: there is more to check:
            // * what does the RDF look like?
            // --> pull it from the atomURI/ConnectionURI and check contents
            // * what does the database look like? */
        }
    }
}
