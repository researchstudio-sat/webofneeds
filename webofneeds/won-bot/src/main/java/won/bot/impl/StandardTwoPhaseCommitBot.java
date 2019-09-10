package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.ParticipantCoordinatorBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.atomlifecycle.CreateAtomWithSocketsAction;
import won.bot.framework.eventbot.action.impl.atomlifecycle.DeactivateAllAtomsOfListAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.socket.TwoPhaseCommitDeactivateOnCloseAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.listener.FinishedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.filter.impl.AtomUriInNamedListFilter;
import won.bot.framework.eventbot.filter.impl.FinishedEventFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.bot.framework.eventbot.listener.impl.WaitForNEventsListener;
import won.protocol.model.SocketType;

/**
 * User: Danijel Date: 15.5.14.
 */
public class StandardTwoPhaseCommitBot extends EventBot {
    protected final int noOfAtoms = 10;
    private static final long MILLIS_BETWEEN_MESSAGES = 100;
    // we use protected members so we can extend the class and
    // access the listeners for unit test assertions and stats
    //
    // we use BaseEventListener as their types so we can access the generic
    // functionality offered by that class
    protected BaseEventListener atomConnector;
    protected BaseEventListener autoOpener;
    protected BaseEventListener participantDeactivator;
    protected BaseEventListener workDoneSignaller;
    protected BaseEventListener creationWaiter;
    protected BaseEventListener coordinatorDeactivator;
    protected BaseEventListener participantAtomCreator;
    protected BaseEventListener coordinatorAtomCreator;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();
        ParticipantCoordinatorBotContextWrapper botContextWrapper = (ParticipantCoordinatorBotContextWrapper) getBotContextWrapper();
        // create atoms every trigger execution until noOfAtoms are created
        this.participantAtomCreator = new ActionOnEventListener(ctx, "participantCreator",
                        new CreateAtomWithSocketsAction(ctx, botContextWrapper.getParticipantListName(),
                                        SocketType.ParticipantSocket.getURI()),
                        noOfAtoms - 1);
        bus.subscribe(ActEvent.class, this.participantAtomCreator);
        // when done, create one coordinator atom
        this.coordinatorAtomCreator = new ActionOnEventListener(ctx, "coordinatorCreator",
                        new FinishedEventFilter(participantAtomCreator),
                        new CreateAtomWithSocketsAction(ctx, botContextWrapper.getCoordinatorListName(),
                                        SocketType.CoordinatorSocket.getURI()),
                        1);
        bus.subscribe(FinishedEvent.class, this.coordinatorAtomCreator);
        // wait for N AtomCreatedEvents
        creationWaiter = new WaitForNEventsListener(ctx, noOfAtoms);
        bus.subscribe(AtomCreatedEvent.class, creationWaiter);
        // when done, connect the participants to the coordinator
        this.atomConnector = new ActionOnEventListener(ctx, "atomConnector", new FinishedEventFilter(creationWaiter),
                        new ConnectFromListToListAction(ctx, botContextWrapper.getCoordinatorListName(),
                                        botContextWrapper.getParticipantListName(),
                                        SocketType.CoordinatorSocket.getURI(), SocketType.ParticipantSocket.getURI(),
                                        MILLIS_BETWEEN_MESSAGES, "Hi!"),
                        1);
        bus.subscribe(FinishedEvent.class, this.atomConnector);
        // add a listener that is informed of the connect/open events and that
        // auto-opens
        // subscribe it to:
        // * connect events - so it responds with open
        // * open events - so it responds with open (if the open received was the first
        // open, and we need need to accept the connection)
        this.autoOpener = new ActionOnEventListener(ctx,
                        new AtomUriInNamedListFilter(ctx, botContextWrapper.getParticipantListName()),
                        new OpenConnectionAction(ctx, "Hi!"));
        bus.subscribe(ConnectFromOtherAtomEvent.class, this.autoOpener);
        // after the last connect event, all connections are closed!
        this.participantDeactivator = new ActionOnEventListener(ctx, "participantDeactivator",
                        new AtomUriInNamedListFilter(ctx, botContextWrapper.getParticipantListName()),
                        new TwoPhaseCommitDeactivateOnCloseAction(ctx), noOfAtoms - 1);
        bus.subscribe(CloseFromOtherAtomEvent.class, this.participantDeactivator);
        coordinatorDeactivator = new ActionOnEventListener(ctx, "coordinatorDeactivator",
                        new FinishedEventFilter(participantDeactivator),
                        new DeactivateAllAtomsOfListAction(ctx, botContextWrapper.getCoordinatorListName()), 1);
        bus.subscribe(FinishedEvent.class, coordinatorDeactivator);
        // add a listener that counts two AtomDeactivatedEvents and then tells the
        // framework that the bot's work is done
        this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, noOfAtoms, new SignalWorkDoneAction(ctx));
        bus.subscribe(AtomDeactivatedEvent.class, this.workDoneSignaller);
    }
}
