package won.bot.integrationtest;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import won.bot.framework.eventbot.event.impl.lifecycle.WorkDoneEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.StandardTwoPhaseCommitBot;

/**
 * User: Danijel Date: 14.5.14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring/app/standardTwoPhaseCommitTest.xml" })
public class StandardTwoPhaseCommitBotTest {
    private static final long ACT_LOOP_TIMEOUT_MILLIS = 100;
    private static final long ACT_LOOP_INITIAL_DELAY_MILLIS = 100;
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
        // create a bot instance and auto-wire it
        AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        this.bot = (MyBot) beanFactory.autowire(MyBot.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
        Object botBean = beanFactory.initializeBean(this.bot, "mybot");
        this.bot = (MyBot) botBean;
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
    public void testStandardTwoPhaseCommitBot() throws Exception {
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
    }

    /**
     * We create a subclass of the bot we want to test here so that we can add a
     * listener to its internal event bus and to access its listeners, which record
     * information during the run that we later check with asserts.
     */
    public static class MyBot extends StandardTwoPhaseCommitBot {
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
            // Coordinator creator
            Assert.assertEquals(1, this.coordinatorAtomCreator.getEventCount());
            Assert.assertEquals(0, this.coordinatorAtomCreator.getExceptionCount());
            // 28 Participants creator
            Assert.assertEquals(noOfAtoms - 1, this.participantAtomCreator.getEventCount());
            Assert.assertEquals(0, this.participantAtomCreator.getExceptionCount());
            // creation waiter
            Assert.assertEquals(noOfAtoms, this.creationWaiter.getEventCount());
            Assert.assertEquals(0, this.creationWaiter.getExceptionCount());
            // Coordinator - Participants connector
            Assert.assertEquals(1, this.atomConnector.getEventCount());
            Assert.assertEquals(0, this.atomConnector.getExceptionCount());
            // Participants deactivator
            Assert.assertEquals(noOfAtoms - 1, this.participantDeactivator.getEventCount());
            Assert.assertEquals(0, this.participantDeactivator.getExceptionCount());
            // Coordinator deactivator
            Assert.assertEquals(1, this.coordinatorDeactivator.getEventCount());
            Assert.assertEquals(0, this.coordinatorDeactivator.getExceptionCount());
            Assert.assertEquals(noOfAtoms, this.workDoneSignaller.getEventCount());
            Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());
            // TODO: there is more to check:
            // * what does the RDF look like?
            // --> pull it from the atomURI/ConnectionURI and check contents
            // * what does the database look like?
        }
    }
}
