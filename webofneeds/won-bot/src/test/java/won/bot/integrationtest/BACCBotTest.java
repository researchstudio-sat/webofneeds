package won.bot.integrationtest;

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
import won.bot.framework.events.event.WorkDoneEvent;
import won.bot.framework.events.listener.ExecuteOnEventListener;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.BACCBot;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 26.2.14.
 * Time: 16.02
 * To change this template use File | Settings | File Templates.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/app/botRunner.xml"})

public class BACCBotTest {
    private static final int RUN_ONCE = 1;
    private static final long ACT_LOOP_TIMEOUT_MILLIS = 1000;
    private static final long ACT_LOOP_INITIAL_DELAY_MILLIS = 1000;

    MyBot bot;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    SpringAwareBotManagerImpl botManager;

    /**
     * This is run before each @Test method.
     */
    @Before
    public void before(){
        //create a bot instance and auto-wire it
        this.bot = (MyBot) applicationContext.getAutowireCapableBeanFactory().autowire(MyBot.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
        //the bot also needs a trigger so its act() method is called regularly.
        // (there is no trigger bean in the context)
        PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
        this.bot.setTrigger(trigger);
    }

    /**
     * The main test method.
     * @throws Exception
     */
    @Test
    public void testBACCBot() throws Exception
    {
        //adding the bot to the bot manager will cause it to be initialized.
        //at that point, the trigger starts.
        botManager.addBot(this.bot);
        //the bot should now be running. We have to wait for it to finish before we
        //can check the results:
        //Together with the barrier.await() in the bot's listener, this trips the barrier
        //and both threads continue.
        this.bot.getBarrier().await();
        //now check the results!
        this.bot.executeAsserts();
    }

    /**
     * We create a subclass of the bot we want to test here so that we can
     * add a listener to its internal event bus and to access its listeners, which
     * record information during the run that we later check with asserts.
     */
    public static class MyBot extends BACCBot
    {
        /**
         * Used for synchronization with the @Test method: it should wait at the
         * barrier until our bot is done, then execute the asserts.
         */
        CyclicBarrier barrier = new CyclicBarrier(2);

        /**
         * Default constructor is required for instantiation through Spring.
         */
        public MyBot(){
        }

        @Override
        protected void initializeEventListeners()
        {
            //of course, let the real bot implementation initialize itself
            super.initializeEventListeners();
            //now, add a listener to the WorkDoneEvent.
            //its only purpose is to trip the CyclicBarrier instance that
            // the test method is waiting on
            getEventBus().subscribe(WorkDoneEvent.class, new ExecuteOnEventListener(getEventListenerContext(), new Runnable(){
                @Override
                public void run()
                {
                    try {
                        //together with the barrier.await() in the @Test method, this trips the barrier
                        //and both threads continue.
                        barrier.await();
                    } catch (Exception e) {
                        logger.warn("caught exception while waiting on barrier", e);
                    }
                }
            }, RUN_ONCE));
        }

        public CyclicBarrier getBarrier()
        {
            return barrier;
        }

        /**
         * Here we check the results of the bot's execution.
         */
        public void executeAsserts()
        {
            //Coordinator creator
            Assert.assertEquals(1, this.coordinatorNeedCreator.getEventCount());
            Assert.assertEquals(0, this.coordinatorNeedCreator.getExceptionCount());
            //28 Participants creator
            Assert.assertEquals(28, this.participantNeedCreator.getEventCount());
            Assert.assertEquals(0, this.participantNeedCreator.getExceptionCount());
            //Coordinator - Participants connector
            Assert.assertEquals(29, this.needConnector.getEventCount());
            Assert.assertEquals(0, this.needConnector.getExceptionCount());
            //13 connect, 13 open
            Assert.assertEquals(2*(14+14), this.autoOpener.getEventCount());
            Assert.assertEquals(0, this.autoOpener.getExceptionCount());
            //messages
            Assert.assertEquals(2*(14+2+3+2+2+4+5+4+3+4+3+3+3+2+9), this.autoResponder.getEventCount());
            Assert.assertEquals(0, this.autoResponder.getExceptionCount());

            Assert.assertEquals(1, this.needDeactivator.getEventCount());
            Assert.assertEquals(0, this.needDeactivator.getExceptionCount());

            //29 needs deactivated
            Assert.assertEquals(29, this.workDoneSignaller.getEventCount());
            Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());

            //TODO: there is more to check:
            //* what does the RDF look like?
            // --> pull it from the needURI/ConnectionURI and check contents
            //* what does the database look like?
        }

    }
}

