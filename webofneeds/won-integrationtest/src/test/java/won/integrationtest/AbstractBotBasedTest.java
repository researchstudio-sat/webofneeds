package won.integrationtest;

import org.junit.Assert;
import org.junit.experimental.categories.Category;
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
import won.bot.framework.bot.Bot;
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
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.extensions.serviceatom.ServiceAtomEnabledBotContextWrapper;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.integrationtest.support.CountdownLatchAction;
import won.protocol.util.WonRdfUtils;
import won.test.category.RequiresDockerServer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AbstractBotBasedTest.Config.class)
@TestPropertySource(locations = { "classpath:/bot-test.properties" }, properties = {
                "WON_NODE_URI=https://wonnode:8443/won",
                "WON_KEYSTORE_DIR=target/bot-keys"
})
@Category(RequiresDockerServer.class)
public abstract class AbstractBotBasedTest
                // uncomment next line to run tests against testcontainers
                extends IntegrationTests {
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
        logger.debug("preparing bot test in runTestWithAsserts(..)");
        MutableStringHolder errorMessage = new MutableStringHolder();
        bot.setInitializer(ctx -> {
            bot.newCountDownLatch(1);
            EventBus bus = ctx.getEventBus();
            bus.clear();
            // now, add a listener to the WorkDoneEvent.
            // its only purpose is to trip the CyclicBarrier instance that
            // the test method is waiting on
            bus.subscribe(TestPassedEvent.class,
                            new ActionOnEventListener(
                                            ctx,
                                            new CountdownLatchAction(ctx, bot.getCountDownLatch())));
            bus.subscribe(TestFailedEvent.class,
                            new ActionOnEventListener(
                                            ctx,
                                            new MultipleActions(ctx, false,
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
                                                            },
                                                            new CountdownLatchAction(ctx, bot.getCountDownLatch()))));
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
        logger.debug("bot re-initialized");
        logger.debug("waiting for test to finish...");
        bot.getCountDownLatch().await();
        if (errorMessage.isPresent()) {
            logger.debug("test failed");
            Assert.fail(errorMessage.get());
        }
        logger.debug("executing asserts (if any)");
        // execute any asserts
        asserter.run();
        logger.debug("test passed");
    }

    protected void passTest(EventBus bus) {
        bus.publish(new TestPassedEvent(bot));
    }

    protected void failTest(EventBus bus, String message) {
        bus.publish(new TestFailedEvent(bot, message));
    }

    protected void failTest(EventBus bus, String message, Exception exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        failTest(bus, message + "\n" + exception.getMessage() + "\n" + sw.toString());
    }

    protected EventListener makeFailureCallbackToFailTest(Bot bot,
                    EventListenerContext ctx, EventBus bus, String action) {
        EventListener failureCallback = event -> {
            String textMessage = WonRdfUtils.MessageUtils
                            .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
            URI atomUri = ((FailureResponseEvent) event).getAtomURI();
            logger.error("{} failed for atom URI {}, original message URI: {}", action,
                            atomUri, textMessage);
            ctx.getBotContextWrapper().removeAtomUri(atomUri);
            bus.publish(new TestFailedEvent(bot,
                            String.format("%s failed for atom URI %s, failure message is: %s",
                                            action, atomUri, textMessage)));
        };
        return failureCallback;
    }

    protected EventListener makeSuccessCallbackToPassTest(Bot bot, EventBus bus, String action) {
        EventListener successCallback = event -> {
            logger.debug("{}} successful, new atom URI is {}", action, ((SuccessResponseEvent) event).getAtomURI());
            bus.publish(new TestPassedEvent(bot));
        };
        return successCallback;
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
        CountDownLatch countDownLatch = new CountDownLatch(2);
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

        public CountDownLatch getCountDownLatch() {
            return countDownLatch;
        }

        public void newCountDownLatch(int num) {
            this.countDownLatch = new CountDownLatch(num);
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
