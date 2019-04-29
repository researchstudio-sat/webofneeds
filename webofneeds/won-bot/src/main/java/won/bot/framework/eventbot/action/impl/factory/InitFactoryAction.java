package won.bot.framework.eventbot.action.impl.factory;

import java.net.URI;
import java.time.Duration;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.bot.framework.component.atomproducer.AtomProducer;
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
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.action.impl.trigger.ActionOnTriggerEventListener;
import won.bot.framework.eventbot.action.impl.trigger.BotTrigger;
import won.bot.framework.eventbot.action.impl.trigger.BotTriggerEvent;
import won.bot.framework.eventbot.action.impl.trigger.StartBotTriggerCommandEvent;
import won.bot.framework.eventbot.action.impl.trigger.StopBotTriggerCommandEvent;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteCreateAtomCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.LogMessageCommandFailureAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateAtomCommandEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateAtomCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateAtomCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryAtomCreationSkippedEvent;
import won.bot.framework.eventbot.event.impl.factory.InitFactoryFinishedEvent;
import won.bot.framework.eventbot.event.impl.factory.StartFactoryAtomCreationEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomProducerExhaustedEvent;
import won.bot.framework.eventbot.filter.impl.TargetCounterFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.protocol.util.WonRdfUtils;

/**
 * Action that creates all atoms from the atomproducer and publishes the
 * InitFactoryFinishedEvent once it is completed
 */
public class InitFactoryAction extends AbstractCreateAtomAction {
    private static int FACTORYATOMCREATION_DURATION_INMILLIS = 250;
    private int targetInFlightCount;
    private int maxInFlightCount;
    private final Counter atomCreationStartedCounter = new CounterImpl("creationStarted");
    private final Counter atomCreationSuccessfulCounter = new CounterImpl("atomsCreated");
    private final Counter atomCreationSkippedCounter = new CounterImpl("atomCreationSkipped");
    private final Counter atomCreationFailedCounter = new CounterImpl("atomCreationFailed");
    private final Counter messagesInFlightCounter = new CounterImpl("messagesInflightCounter");

    public InitFactoryAction(EventListenerContext eventListenerContext, URI... sockets) {
        this(eventListenerContext, 30, 50, sockets);
    }

    public InitFactoryAction(EventListenerContext eventListenerContext, int targetInFlightCount, int maxInFlightCount,
                    URI... sockets) {
        super(eventListenerContext, null, false, false, sockets);
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
        // create a targeted counter that will publish an event when the target is
        // reached
        // in this case, 0 unfinished atom creations means that all atoms were created
        final TargetCounterDecorator creationUnfinishedCounter = new TargetCounterDecorator(ctx,
                        new CounterImpl("creationUnfinished"), 0);
        BotTrigger createFactoryAtomTrigger = new BotTrigger(ctx,
                        Duration.ofMillis(FACTORYATOMCREATION_DURATION_INMILLIS));
        createFactoryAtomTrigger.activate();
        bus.subscribe(StartFactoryAtomCreationEvent.class, new ActionOnFirstEventListener(ctx,
                        new PublishEventAction(ctx, new StartBotTriggerCommandEvent(createFactoryAtomTrigger))));
        bus.subscribe(BotTriggerEvent.class,
                        new ActionOnTriggerEventListener(ctx, createFactoryAtomTrigger, new BaseEventBotAction(ctx) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                if (isTooManyMessagesInFlight(messagesInFlightCounter)) {
                                    return;
                                }
                                adjustTriggerInterval(createFactoryAtomTrigger, messagesInFlightCounter);
                                AtomProducer atomProducer = ctx.getAtomProducer(); // defined via spring
                                Dataset dataset = atomProducer.create();
                                if (dataset == null && atomProducer.isExhausted()) {
                                    bus.publish(new AtomProducerExhaustedEvent());
                                    bus.unsubscribe(executingListener);
                                    return;
                                }
                                URI atomUriFromProducer = null;
                                Resource atomResource = WonRdfUtils.AtomUtils.getAtomResource(dataset);
                                if (atomResource.isURIResource()) {
                                    atomUriFromProducer = URI.create(atomResource.getURI());
                                }
                                if (atomUriFromProducer != null) {
                                    URI atomURI = botContextWrapper.getURIFromInternal(atomUriFromProducer);
                                    if (atomURI != null) {
                                        bus.publish(new FactoryAtomCreationSkippedEvent());
                                    } else {
                                        bus.publish(new CreateAtomCommandEvent(dataset,
                                                        botContextWrapper.getFactoryListName(), usedForTesting,
                                                        doNotMatch));
                                    }
                                }
                            }
                        }));
        bus.subscribe(CreateAtomCommandSuccessEvent.class, new ActionOnEventListener(ctx,
                        new MultipleActions(ctx, new DecrementCounterAction(ctx, creationUnfinishedCounter), // decrease
                                                                                                             // the
                                                                                                             // creationUnfinishedCounter
                                        new IncrementCounterAction(ctx, atomCreationSuccessfulCounter), // count a
                                                                                                        // successful
                                                                                                        // atom creation
                                        new BaseEventBotAction(ctx) {
                                            @Override
                                            protected void doRun(Event event, EventListener executingListener)
                                                            throws Exception {
                                                if (event instanceof CreateAtomCommandSuccessEvent) {
                                                    CreateAtomCommandSuccessEvent atomCreatedEvent = (CreateAtomCommandSuccessEvent) event;
                                                    botContextWrapper.addInternalIdToUriReference(
                                                                    atomCreatedEvent.getAtomUriBeforeCreation(),
                                                                    atomCreatedEvent.getAtomURI());
                                                }
                                            }
                                        })));
        bus.subscribe(CreateAtomCommandEvent.class,
                        new ActionOnEventListener(ctx, new MultipleActions(ctx, new ExecuteCreateAtomCommandAction(ctx), // execute
                                                                                                                         // the
                                                                                                                         // atom
                                                                                                                         // creation
                                                                                                                         // for
                                                                                                                         // the
                                                                                                                         // atom
                                                                                                                         // in
                                                                                                                         // the
                                                                                                                         // event
                                        new IncrementCounterAction(ctx, atomCreationStartedCounter), // increase the
                                                                                                     // atomCreationStartedCounter
                                                                                                     // to
                                                                                                     // signal another
                                                                                                     // pending creation
                                        new IncrementCounterAction(ctx, creationUnfinishedCounter) // increase the
                                                                                                   // creationUnfinishedCounter
                                                                                                   // to
                                                                                                   // signal another
                                                                                                   // pending creation
                        )));
        // if an atom is already created we skip the recreation of it and increase the
        // atomCreationSkippedCounter
        bus.subscribe(FactoryAtomCreationSkippedEvent.class,
                        new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, atomCreationSkippedCounter)));
        // if a creation failed, we don't want to keep us from keeping the correct count
        bus.subscribe(CreateAtomCommandFailureEvent.class,
                        new ActionOnEventListener(ctx,
                                        new MultipleActions(ctx,
                                                        new DecrementCounterAction(ctx, creationUnfinishedCounter), // decrease
                                                                                                                    // the
                                                                                                                    // creationUnfinishedCounter
                                                        new IncrementCounterAction(ctx, atomCreationFailedCounter) // count
                                                                                                                   // an
                                                                                                                   // unsuccessful
                                                                                                                   // atom
                                                                                                                   // creation
                                        )));
        // when the atomproducer is exhausted, we stop the creator (trigger) and we have
        // to wait until all unfinished atom creations finish
        // when they do, the InitFactoryFinishedEvent is published
        bus.subscribe(AtomProducerExhaustedEvent.class,
                        new ActionOnFirstEventListener(ctx, new MultipleActions(ctx,
                                        new PublishEventAction(ctx,
                                                        new StopBotTriggerCommandEvent(createFactoryAtomTrigger)),
                                        new BaseEventBotAction(ctx) {
                                            @Override
                                            protected void doRun(Event event, EventListener executingListener)
                                                            throws Exception {
                                                // when we're called, there probably are atom creations unfinished, but
                                                // there
                                                // may not be
                                                // a)
                                                // first, prepare for the case when there are unfinished atom creations:
                                                // we register a listener, waiting for the unfinished counter to reach 0
                                                EventListener waitForUnfinishedAtomsListener = new ActionOnFirstEventListener(
                                                                ctx, new TargetCounterFilter(creationUnfinishedCounter),
                                                                new PublishEventAction(ctx,
                                                                                new InitFactoryFinishedEvent()));
                                                bus.subscribe(TargetCountReachedEvent.class,
                                                                waitForUnfinishedAtomsListener);
                                                // now, we can check if we've already reached the target
                                                if (creationUnfinishedCounter.getCount() <= 0) {
                                                    // ok, turned out we didn't need that listener
                                                    bus.unsubscribe(waitForUnfinishedAtomsListener);
                                                    bus.publish(new InitFactoryFinishedEvent());
                                                }
                                            }
                                        })));
        bus.subscribe(InitFactoryFinishedEvent.class,
                        new ActionOnFirstEventListener(ctx, "factoryCreateStatsLogger", new BaseEventBotAction(ctx) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                logger.info("FactoryAtomCreation finished: total:{}, successful: {}, failed: {}, skipped: {}",
                                                new Object[] { atomCreationStartedCounter.getCount(),
                                                                atomCreationSuccessfulCounter.getCount(),
                                                                atomCreationFailedCounter.getCount(),
                                                                atomCreationSkippedCounter.getCount() });
                            }
                        }));
        // MessageInFlight counter handling *************************
        bus.subscribe(MessageCommandEvent.class,
                        new ActionOnEventListener(ctx, new IncrementCounterAction(ctx, messagesInFlightCounter)));
        bus.subscribe(MessageCommandResultEvent.class,
                        new ActionOnEventListener(ctx, new DecrementCounterAction(ctx, messagesInFlightCounter)));
        bus.subscribe(MessageCommandFailureEvent.class,
                        new ActionOnEventListener(ctx, new LogMessageCommandFailureAction(ctx))); // if we receive a
                                                                                                  // message command
                                                                                                  // failure, log it
        // Start the atom creation stuff
        bus.publish(new StartFactoryAtomCreationEvent());
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
