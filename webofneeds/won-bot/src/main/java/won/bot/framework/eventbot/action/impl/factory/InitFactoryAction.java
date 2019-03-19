package won.bot.framework.eventbot.action.impl.factory;

import java.net.URI;
import java.time.Duration;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.MultipleActions;
import won.bot.framework.eventbot.action.impl.PublishEventAction;
import won.bot.framework.eventbot.action.impl.counter.Counter;
import won.bot.framework.eventbot.action.impl.counter.CounterImpl;
import won.bot.framework.eventbot.action.impl.counter.DecrementCounterAction;
import won.bot.framework.eventbot.action.impl.counter.IncrementCounterAction;
import won.bot.framework.eventbot.action.impl.counter.TargetCountReachedEvent;
import won.bot.framework.eventbot.action.impl.counter.TargetCounterDecorator;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.action.impl.trigger.ActionOnTriggerEventListener;
import won.bot.framework.eventbot.action.impl.trigger.BotTrigger;
import won.bot.framework.eventbot.action.impl.trigger.BotTriggerEvent;
import won.bot.framework.eventbot.action.impl.trigger.StartBotTriggerCommandEvent;
import won.bot.framework.eventbot.action.impl.trigger.StopBotTriggerCommandEvent;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteCreateNeedCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.LogMessageCommandFailureAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryNeedCreationSkippedEvent;
import won.bot.framework.eventbot.event.impl.factory.InitFactoryFinishedEvent;
import won.bot.framework.eventbot.event.impl.factory.StartFactoryNeedCreationEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedProducerExhaustedEvent;
import won.bot.framework.eventbot.filter.impl.TargetCounterFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.protocol.util.WonRdfUtils;

/**
 * Action that creates all needs from the needproducer and publishes the InitFactoryFinishedEvent once it is completed
 */
public class InitFactoryAction extends AbstractCreateNeedAction {
    private static int FACTORYNEEDCREATION_DURATION_INMILLIS = 250;

    private int targetInFlightCount;
    private int maxInFlightCount;

    private final Counter needCreationStartedCounter = new CounterImpl("creationStarted");
    private final Counter needCreationSuccessfulCounter = new CounterImpl("needsCreated");
    private final Counter needCreationSkippedCounter = new CounterImpl("needCreationSkipped");
    private final Counter needCreationFailedCounter = new CounterImpl("needCreationFailed");
    private final Counter messagesInFlightCounter = new CounterImpl("messagesInflightCounter");

    public InitFactoryAction(EventListenerContext eventListenerContext, URI... facets) {
        this(eventListenerContext, 30, 50, facets);
    }

    public InitFactoryAction(EventListenerContext eventListenerContext, int targetInFlightCount, int maxInFlightCount,
            URI... facets) {
        super(eventListenerContext, null, false, false, facets);
        this.targetInFlightCount = targetInFlightCount;
        this.maxInFlightCount = maxInFlightCount;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof InitializeEvent)
                || !(getEventListenerContext().getBotContextWrapper() instanceof FactoryBotContextWrapper)) {
            logger.error("InitFactoryAction can only handle InitializeEvent with FactoryBotContextWrapper");
            return;
        }

        final boolean usedForTesting = this.usedForTesting;
        final boolean doNotMatch = this.doNotMatch;

        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = ctx.getEventBus();
        FactoryBotContextWrapper botContextWrapper = (FactoryBotContextWrapper) ctx.getBotContextWrapper();

        // create a targeted counter that will publish an event when the target is reached
        // in this case, 0 unfinished need creations means that all needs were created
        final TargetCounterDecorator creationUnfinishedCounter = new TargetCounterDecorator(ctx,
                new CounterImpl("creationUnfinished"), 0);

        BotTrigger createFactoryNeedTrigger = new BotTrigger(ctx,
                Duration.ofMillis(FACTORYNEEDCREATION_DURATION_INMILLIS));
        createFactoryNeedTrigger.activate();
        bus.subscribe(StartFactoryNeedCreationEvent.class, new ActionOnFirstEventListener(ctx,
                new PublishEventAction(ctx, new StartBotTriggerCommandEvent(createFactoryNeedTrigger))));
        bus.subscribe(BotTriggerEvent.class,
                new ActionOnTriggerEventListener(ctx, createFactoryNeedTrigger, new BaseEventBotAction(ctx) {
                    @Override
                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                        if (isTooManyMessagesInFlight(messagesInFlightCounter)) {
                            return;
                        }
                        adjustTriggerInterval(createFactoryNeedTrigger, messagesInFlightCounter);
                        NeedProducer needProducer = ctx.getNeedProducer(); // defined via spring
                        Dataset dataset = needProducer.create();

                        if (dataset == null && needProducer.isExhausted()) {
                            bus.publish(new NeedProducerExhaustedEvent());
                            bus.unsubscribe(executingListener);
                            return;
                        }
                        URI needUriFromProducer = null;
                        Resource needResource = WonRdfUtils.NeedUtils.getNeedResource(dataset);

                        if (needResource.isURIResource()) {
                            needUriFromProducer = URI.create(needResource.getURI());
                        }
                        if (needUriFromProducer != null) {

                            URI needURI = botContextWrapper.getURIFromInternal(needUriFromProducer);

                            if (needURI != null) {
                                bus.publish(new FactoryNeedCreationSkippedEvent());
                            } else {
                                bus.publish(new CreateNeedCommandEvent(dataset, botContextWrapper.getFactoryListName(),
                                        usedForTesting, doNotMatch));
                            }
                        }
                    }
                }));

        bus.subscribe(CreateNeedCommandSuccessEvent.class,
                new ActionOnEventListener(ctx,
                        new MultipleActions(ctx, new DecrementCounterAction(ctx, creationUnfinishedCounter), // decrease
                                                                                                             // the
                                                                                                             // creationUnfinishedCounter
                                new IncrementCounterAction(ctx, needCreationSuccessfulCounter), // count a successful
                                                                                                // need creation
                                new BaseEventBotAction(ctx) {
                                    @Override
                                    protected void doRun(Event event, EventListener executingListener)
                                            throws Exception {
                                        if (event instanceof CreateNeedCommandSuccessEvent) {
                                            CreateNeedCommandSuccessEvent needCreatedEvent = (CreateNeedCommandSuccessEvent) event;
                                            botContextWrapper.addInternalIdToUriReference(
                                                    needCreatedEvent.getNeedUriBeforeCreation(),
                                                    needCreatedEvent.getNeedURI());
                                        }
                                    }
                                })));

        bus.subscribe(CreateNeedCommandEvent.class,
                new ActionOnEventListener(ctx, new MultipleActions(ctx, new ExecuteCreateNeedCommandAction(ctx), // execute
                                                                                                                 // the
                                                                                                                 // need
                                                                                                                 // creation
                                                                                                                 // for
                                                                                                                 // the
                                                                                                                 // need
                                                                                                                 // in
                                                                                                                 // the
                                                                                                                 // event
                        new IncrementCounterAction(ctx, needCreationStartedCounter), // increase the
                                                                                     // needCreationStartedCounter to
                                                                                     // signal another pending creation
                        new IncrementCounterAction(ctx, creationUnfinishedCounter) // increase the
                                                                                   // creationUnfinishedCounter to
                                                                                   // signal another pending creation
                )));

        // if a need is already created we skip the recreation of it and increase the needCreationSkippedCounter
        bus.subscribe(FactoryNeedCreationSkippedEvent.class,
                new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, needCreationSkippedCounter)));

        // if a creation failed, we don't want to keep us from keeping the correct count
        bus.subscribe(CreateNeedCommandFailureEvent.class,
                new ActionOnEventListener(ctx,
                        new MultipleActions(ctx, new DecrementCounterAction(ctx, creationUnfinishedCounter), // decrease
                                                                                                             // the
                                                                                                             // creationUnfinishedCounter
                                new IncrementCounterAction(ctx, needCreationFailedCounter) // count an unsuccessful need
                                                                                           // creation
                        )));

        // when the needproducer is exhausted, we stop the creator (trigger) and we have to wait until all unfinished
        // need creations finish
        // when they do, the InitFactoryFinishedEvent is published
        bus.subscribe(NeedProducerExhaustedEvent.class,
                new ActionOnFirstEventListener(ctx,
                        new MultipleActions(ctx,
                                new PublishEventAction(ctx, new StopBotTriggerCommandEvent(createFactoryNeedTrigger)),
                                new BaseEventBotAction(ctx) {
                                    @Override
                                    protected void doRun(Event event, EventListener executingListener)
                                            throws Exception {
                                        // when we're called, there probably are need creations unfinished, but there
                                        // may not be
                                        // a)
                                        // first, prepare for the case when there are unfinished need creations:
                                        // we register a listener, waiting for the unfinished counter to reach 0
                                        EventListener waitForUnfinishedNeedsListener = new ActionOnFirstEventListener(
                                                ctx, new TargetCounterFilter(creationUnfinishedCounter),
                                                new PublishEventAction(ctx, new InitFactoryFinishedEvent()));
                                        bus.subscribe(TargetCountReachedEvent.class, waitForUnfinishedNeedsListener);
                                        // now, we can check if we've already reached the target
                                        if (creationUnfinishedCounter.getCount() <= 0) {
                                            // ok, turned out we didn't need that listener
                                            bus.unsubscribe(waitForUnfinishedNeedsListener);
                                            bus.publish(new InitFactoryFinishedEvent());
                                        }
                                    }
                                })));
        bus.subscribe(InitFactoryFinishedEvent.class,
                new ActionOnFirstEventListener(ctx, "factoryCreateStatsLogger", new BaseEventBotAction(ctx) {
                    @Override
                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                        logger.info("FactoryNeedCreation finished: total:{}, successful: {}, failed: {}, skipped: {}",
                                new Object[] { needCreationStartedCounter.getCount(),
                                        needCreationSuccessfulCounter.getCount(), needCreationFailedCounter.getCount(),
                                        needCreationSkippedCounter.getCount() });
                    }
                }));

        // MessageInFlight counter handling *************************
        bus.subscribe(MessageCommandEvent.class,
                new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, messagesInFlightCounter)));
        bus.subscribe(MessageCommandResultEvent.class,
                new ActionOnEventListener(ctx, new DecrementCounterAction(ctx, messagesInFlightCounter)));

        bus.subscribe(MessageCommandFailureEvent.class,
                new ActionOnEventListener(ctx, new LogMessageCommandFailureAction(ctx))); // if we receive a message
                                                                                          // command failure, log it

        // Start the need creation stuff
        bus.publish(new StartFactoryNeedCreationEvent());
    }

    private boolean isTooManyMessagesInFlight(Counter messagesInFlightCounter) {
        return messagesInFlightCounter.getCount() > maxInFlightCount;
    }

    private void adjustTriggerInterval(BotTrigger createConnectionsTrigger, Counter targetCounter) {
        // change interval to achieve desired inflight count
        int desiredInFlightCount = targetInFlightCount;
        int inFlightCountDiff = targetCounter.getCount() - desiredInFlightCount;
        double factor = (double) inFlightCountDiff / (double) desiredInFlightCount;
        createConnectionsTrigger.changeIntervalByFactor(1 + 0.001 * factor);
    }
}
