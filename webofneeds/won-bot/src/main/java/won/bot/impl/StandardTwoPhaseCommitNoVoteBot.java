package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.ParticipantCoordinatorBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.atomlifecycle.CreateAtomWithSocketsAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.socket.TwoPhaseCommitNoVoteDeactivateAllAtomsAction;
import won.bot.framework.eventbot.action.impl.wonmessage.CloseConnectionAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.listener.FinishedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.filter.impl.FinishedEventFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.protocol.model.SocketType;

/**
 * User: Danijel Date: 15.5.14.
 */
public class StandardTwoPhaseCommitNoVoteBot extends EventBot {
    protected final int noOfAtoms = 10;
    private static final long MILLIS_BETWEEN_MESSAGES = 100;
    // we use protected members so we can extend the class and
    // access the listeners for unit test assertions and stats
    //
    // we use BaseEventListener as their types so we can access the generic
    // functionality offered by that class
    protected BaseEventListener atomConnector;
    protected BaseEventListener autoOpener;
    protected BaseEventListener autoCloser;
    protected BaseEventListener atomDeactivator;
    protected BaseEventListener workDoneSignaller;
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
        // when done, connect the participants to the coordinator
        this.atomConnector = new ActionOnceAfterNEventsListener(ctx, "atomConnector", noOfAtoms,
                        new ConnectFromListToListAction(ctx, botContextWrapper.getCoordinatorListName(),
                                        botContextWrapper.getParticipantListName(),
                                        SocketType.CoordinatorSocket.getURI(), SocketType.ParticipantSocket.getURI(),
                                        MILLIS_BETWEEN_MESSAGES, "Hi!"));
        bus.subscribe(AtomCreatedEvent.class, this.atomConnector);
        // add a listener that is informed of the connect/open events and that
        // auto-opens
        // subscribe it to:
        // * connect events - so it responds with open
        // * open events - so it responds with open (if the open received was the first
        // open, and we still need to accept the connection)
        this.autoOpener = new ActionOnEventListener(ctx, "autoOpener", new OpenConnectionAction(ctx, "Hi!"));
        bus.subscribe(OpenFromOtherAtomEvent.class, this.autoOpener);
        bus.subscribe(ConnectFromOtherAtomEvent.class, this.autoOpener);
        this.autoCloser = new ActionOnceAfterNEventsListener(ctx, "autoCloser", noOfAtoms - 3,
                        new CloseConnectionAction(ctx, "Bye!"));
        bus.subscribe(ConnectFromOtherAtomEvent.class, this.autoCloser);
        // after the last connect event, all connections are closed!
        this.atomDeactivator = new ActionOnEventListener(ctx, new TwoPhaseCommitNoVoteDeactivateAllAtomsAction(ctx), 1);
        bus.subscribe(CloseFromOtherAtomEvent.class, this.atomDeactivator);
        // add a listener that counts two AtomDeactivatedEvents and then tells the
        // framework that the bot's work is done
        this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, noOfAtoms, new SignalWorkDoneAction(ctx));
        bus.subscribe(AtomDeactivatedEvent.class, this.workDoneSignaller);
    }
}
