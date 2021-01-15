package won.integrationtest;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.component.atomproducer.AtomProducer;
import won.bot.framework.component.atomproducer.impl.NopAtomProducer;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.MultipleActions;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.test.TestFailedEvent;
import won.bot.framework.eventbot.event.impl.test.TestPassedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.extensions.serviceatom.ServiceAtomEnabledBotContextWrapper;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.integrationtest.support.TripBarrierAction;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AbstractBotBasedTest.Config.class)
@TestPropertySource(locations = { "classpath:/bot-test.properties" }, properties = {
                "WON_NODE_URI=https://wonnode:8443/won",
                "WON_KEYSTORE_DIR=target/bot-keys"
})
// @Category(RequiresDockerServer.class)
public abstract class AbstractBotBasedTest
// uncomment next line to run tests against testcontainers
// extends IntegrationTests
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final long ACT_LOOP_TIMEOUT_MILLIS = 100;
    private static final long ACT_LOOP_INITIAL_DELAY_MILLIS = 100;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    SpringAwareBotManagerImpl botManager;
    @Value("${botContext.impl}")
    String botContextBean;
    @Autowired
    MyBot bot;

    protected void runTest(Consumer<EventListenerContext> botInitializer) throws Exception {
        runTestWithAsserts(botInitializer, () -> {
        });
    }

    protected void runTestWithAsserts(Consumer<EventListenerContext> botInitializer, Runnable asserter)
                    throws Exception {
        MutableStringHolder errorMessage = new MutableStringHolder();
        bot.setInitializer(ctx -> {
            bot.getBarrier().reset();
            EventBus bus = ctx.getEventBus();
            bus.clear();
            // now, add a listener to the WorkDoneEvent.
            // its only purpose is to trip the CyclicBarrier instance that
            // the test method is waiting on
            bus.subscribe(TestPassedEvent.class,
                            new ActionOnEventListener(
                                            ctx,
                                            new TripBarrierAction(ctx, bot.getBarrier())));
            bus.subscribe(TestFailedEvent.class,
                            new ActionOnEventListener(
                                            ctx,
                                            new MultipleActions(ctx,
                                                            new TripBarrierAction(ctx, bot.getBarrier()),
                                                            new BaseEventBotAction(ctx) {
                                                                @Override
                                                                protected void doRun(Event event,
                                                                                EventListener executingListener)
                                                                                throws Exception {
                                                                    if (event instanceof TestFailedEvent) {
                                                                        errorMessage.set(((TestFailedEvent) event)
                                                                                        .getMessage());
                                                                        if (errorMessage.isEmpty()) {
                                                                            errorMessage.set(
                                                                                            "[no error message provided]");
                                                                        }
                                                                    }
                                                                }
                                                            })));
            // now run test-specific initializer
            if (botInitializer != null) {
                botInitializer.accept(ctx);
            }
        });
        // possibly, the bot is already running (the SpringAwareBotManager finds and
        // initializes it)
        // make sure we can re-initialize it here
        bot.shutdown();
        // re-initialize
        bot.initialize();
        try {
            bot.getBarrier().await();
            if (errorMessage.isPresent()) {
                Assert.fail(errorMessage.get());
            }
            // execute any asserts
            asserter.run();
        } catch (InterruptedException e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        } catch (BrokenBarrierException e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Configuration
    @ImportResource("classpath:/spring/app/botRunner.xml")
    static class Config {
        @Value("${bot.name}")
        String botName = MethodHandles.lookup().lookupClass().getSimpleName();

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public BotContextWrapper serviceAtomBotContextWrapper(@Autowired BotContext botContext) {
            return new ServiceAtomEnabledBotContextWrapper(botContext, botName);
        }

        @Bean(name = "atomProducer")
        @Primary
        public AtomProducer atomProducer() {
            return new NopAtomProducer();
        }

        @Bean
        public PeriodicTrigger botTrigger() {
            PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
            return trigger;
        }

        @Bean
        public MyBot myBot() {
            return new MyBot();
        }
    }

    /**
     * We create a subclass of the bot we want to test here so that we can add a
     * listener to its internal event bus and to access its listeners, which record
     * information during the run that we later check with asserts.
     */
    public static class MyBot extends EventBot {
        /**
         * Used for synchronization with the @Test method: it should wait at the barrier
         * until our bot is done, then execute the asserts.
         */
        CyclicBarrier barrier = new CyclicBarrier(2);
        private Consumer<EventListenerContext> initializer;

        /**
         * Default constructor is required for instantiation through Spring.
         */
        public MyBot() {
        }

        public void setInitializer(Consumer<EventListenerContext> initializer) {
            this.initializer = initializer;
        }

        @Override
        protected void initializeEventListeners() {
            if (this.initializer != null) {
                this.initializer.accept(this.getEventListenerContext());
            }
        }

        public CyclicBarrier getBarrier() {
            return barrier;
        }
    }

    private static class MutableStringHolder {
        private String content = null;

        public MutableStringHolder() {
        }

        public MutableStringHolder(String content) {
            this.content = content;
        }

        public void set(String content) {
            this.content = content;
        }

        public String get() {
            return content;
        }

        public boolean isPresent() {
            return content != null;
        }

        public boolean isEmpty() {
            return content == null;
        }
    }
}
