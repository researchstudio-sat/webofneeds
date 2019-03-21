package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.ParticipantCoordinatorBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.facet.TwoPhaseCommitDeactivateOnCloseAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.CreateNeedWithFacetsAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.DeactivateAllNeedsOfListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.listener.FinishedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.impl.FinishedEventFilter;
import won.bot.framework.eventbot.filter.impl.NeedUriInNamedListFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.bot.framework.eventbot.listener.impl.WaitForNEventsListener;
import won.protocol.model.FacetType;

/**
 * User: Danijel Date: 15.5.14.
 */
public class StandardTwoPhaseCommitBot extends EventBot {

  protected final int noOfNeeds = 10;
  private static final long MILLIS_BETWEEN_MESSAGES = 100;

  // we use protected members so we can extend the class and
  // access the listeners for unit test assertions and stats
  //
  // we use BaseEventListener as their types so we can access the generic
  // functionality offered by that class
  protected BaseEventListener needConnector;
  protected BaseEventListener autoOpener;
  protected BaseEventListener participantDeactivator;
  protected BaseEventListener workDoneSignaller;
  protected BaseEventListener creationWaiter;
  protected BaseEventListener coordinatorDeactivator;

  protected BaseEventListener participantNeedCreator;
  protected BaseEventListener coordinatorNeedCreator;

  @Override
  protected void initializeEventListeners() {
    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    ParticipantCoordinatorBotContextWrapper botContextWrapper = (ParticipantCoordinatorBotContextWrapper) getBotContextWrapper();

    // create needs every trigger execution until noOfNeeds are created
    this.participantNeedCreator = new ActionOnEventListener(ctx, "participantCreator", new CreateNeedWithFacetsAction(
        ctx, botContextWrapper.getParticipantListName(), FacetType.ParticipantFacet.getURI()), noOfNeeds - 1);
    bus.subscribe(ActEvent.class, this.participantNeedCreator);

    // when done, create one coordinator need
    this.coordinatorNeedCreator = new ActionOnEventListener(ctx, "coordinatorCreator",
        new FinishedEventFilter(participantNeedCreator), new CreateNeedWithFacetsAction(ctx,
            botContextWrapper.getCoordinatorListName(), FacetType.CoordinatorFacet.getURI()),
        1);
    bus.subscribe(FinishedEvent.class, this.coordinatorNeedCreator);

    // wait for N NeedCreatedEvents
    creationWaiter = new WaitForNEventsListener(ctx, noOfNeeds);
    bus.subscribe(NeedCreatedEvent.class, creationWaiter);

    // when done, connect the participants to the coordinator
    this.needConnector = new ActionOnEventListener(ctx, "needConnector", new FinishedEventFilter(creationWaiter),
        new ConnectFromListToListAction(ctx, botContextWrapper.getCoordinatorListName(),
            botContextWrapper.getParticipantListName(), FacetType.CoordinatorFacet.getURI(),
            FacetType.ParticipantFacet.getURI(), MILLIS_BETWEEN_MESSAGES, "Hi!"),
        1);
    bus.subscribe(FinishedEvent.class, this.needConnector);

    // add a listener that is informed of the connect/open events and that
    // auto-opens
    // subscribe it to:
    // * connect events - so it responds with open
    // * open events - so it responds with open (if the open received was the first
    // open, and we still need to accept the connection)
    this.autoOpener = new ActionOnEventListener(ctx,
        new NeedUriInNamedListFilter(ctx, botContextWrapper.getParticipantListName()),
        new OpenConnectionAction(ctx, "Hi!"));
    bus.subscribe(ConnectFromOtherNeedEvent.class, this.autoOpener);

    // after the last connect event, all connections are closed!
    this.participantDeactivator = new ActionOnEventListener(ctx, "participantDeactivator",
        new NeedUriInNamedListFilter(ctx, botContextWrapper.getParticipantListName()),
        new TwoPhaseCommitDeactivateOnCloseAction(ctx), noOfNeeds - 1);
    bus.subscribe(CloseFromOtherNeedEvent.class, this.participantDeactivator);

    coordinatorDeactivator = new ActionOnEventListener(ctx, "coordinatorDeactivator",
        new FinishedEventFilter(participantDeactivator),
        new DeactivateAllNeedsOfListAction(ctx, botContextWrapper.getCoordinatorListName()), 1);
    bus.subscribe(FinishedEvent.class, coordinatorDeactivator);

    // add a listener that counts two NeedDeactivatedEvents and then tells the
    // framework that the bot's work is done
    this.workDoneSignaller = new ActionOnceAfterNEventsListener(ctx, noOfNeeds, new SignalWorkDoneAction(ctx));
    bus.subscribe(NeedDeactivatedEvent.class, this.workDoneSignaller);
  }

}
