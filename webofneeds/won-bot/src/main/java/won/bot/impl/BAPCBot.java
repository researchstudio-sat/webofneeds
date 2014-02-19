package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.event.*;
import won.bot.framework.events.listener.*;
import won.protocol.model.FacetType;


/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 12.2.14.
 * Time: 20.45
 * To change this template use File | Settings | File Templates.
 */
public class BAPCBot extends EventBot {
    private static final int NO_OF_NEEDS = 5;
    private static final int NO_OF_MESSAGES = 10;
    private static final long MILLIS_BETWEEN_MESSAGES = 1000;
    private static final String URI_LIST_NAME_PARTICIPANT = "participants";
    private static final String URI_LIST_NAME_COORDINATOR = "coordinator";

    //we use protected members so we can extend the class and
    //access the listeners for unit test assertions and stats
    //
    //we use BaseEventListener as their types so we can access the generic
    //functionality offered by that class
    protected BaseEventListener participantNeedCreator;
    protected BaseEventListener coordinatorNeedCreator;
    protected BaseEventListener needConnector;
    protected BaseEventListener autoOpener;
    protected BaseEventListener autoResponder;
    protected BaseEventListener connectionCloser;
    protected BaseEventListener needDeactivator;
    protected BaseEventListener workDoneSignaller;

    @Override
    protected void initializeEventListeners()
    {
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();

        //create needs every trigger execution until NO_OF_NEEDS are created
        this.participantNeedCreator = new ExecuteOnEventListener(
                ctx,
                new EventBotActions.CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_PARTICIPANT, FacetType.BAPCParticipantFacet.getURI()),
                NO_OF_NEEDS - 1
        );
        bus.subscribe(ActEvent.class,this.participantNeedCreator);
        //create needs every trigger execution until NO_OF_NEEDS are created
        this.coordinatorNeedCreator = new ExecuteOnEventListener(
                ctx,
                new EventBotActions.CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_COORDINATOR, FacetType.BAPCCoordinatorFacet.getURI()),
                1
        );
        bus.subscribe(ActEvent.class,this.coordinatorNeedCreator);
        //count until NO_OF_NEEDS were created, then
        //   * connect the Coordinator with Participant needs
        this.needConnector = new ExecuteOnceAfterNEventsListener(ctx,
                new EventBotActions.ConnectFromListToListAction(
                        ctx, URI_LIST_NAME_COORDINATOR, URI_LIST_NAME_PARTICIPANT, FacetType.BAPCCoordinatorFacet.getURI(), FacetType.BAPCParticipantFacet.getURI()),NO_OF_NEEDS);
        bus.subscribe(NeedCreatedEvent.class, this.needConnector);

        //add a listener that is informed of the connect/open events and that auto-opens
        //subscribe it to:
        // * connect events - so it responds with open
        // * open events - so it responds with open (if the open received was the first open, and we still need to accept the connection)
        this.autoOpener = new AutomaticConnectionOpenerListener(ctx);
        bus.subscribe(OpenFromOtherNeedEvent.class, this.autoOpener);
        bus.subscribe(ConnectFromOtherNeedEvent.class, this.autoOpener);

        //add a listener that auto-responds to messages by a message
        //after NO_OF_MESSAGES messages, it unsubscribes from all events
        //subscribe it to:
        // * message events - so it responds
        // * open events - so it initiates the chain reaction of responses
        this.autoResponder = new AutomaticBAMessageResponderListener(ctx, NO_OF_MESSAGES, MILLIS_BETWEEN_MESSAGES);
        bus.subscribe(OpenFromOtherNeedEvent.class, this.autoResponder);
        bus.subscribe(MessageFromOtherNeedEvent.class, this.autoResponder);

        //add a listener that closes the connection after it has seen NO_OF_MESSAGES messages
        this.connectionCloser = new DelegateOnceAfterNEventsListener(
                ctx,
                NO_OF_MESSAGES,
                new CloseConnectionListener(ctx));
        bus.subscribe(MessageFromOtherNeedEvent.class, this.connectionCloser);

        //add a listener that auto-responds to a close message with a deactivation of both needs.
        //subscribe it to:
        // * close events
        this.needDeactivator = new DeactivateNeedOnConnectionCloseListener(ctx);
        bus.subscribe(CloseFromOtherNeedEvent.class, this.needDeactivator);

        //add a listener that counts two NeedDeactivatedEvents and then tells the
        //framework that the bot's work is done
        this.workDoneSignaller = new ExecuteOnceAfterNEventsListener(
                ctx,
                new EventBotActions.SignalWorkDoneAction(ctx), NO_OF_NEEDS
        );
        bus.subscribe(NeedDeactivatedEvent.class, this.workDoneSignaller);
    }
}




















