package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.event.*;
import won.bot.framework.events.listener.*;
import won.bot.framework.events.listener.baStateBots.BATestBotScript;


import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicCoordinationMessageAsTextBots.*;
import won.protocol.model.FacetType;

import java.util.ArrayList;
import java.util.List;
/**
 * User: Danijel
 * Date: 2.4.14.
 */
public class BAAtomicCCBot extends EventBot {
    private static final int NO_OF_NEEDS = 5;
    private static final int NO_OF_MESSAGES = 50;
    private static final long MILLIS_BETWEEN_MESSAGES = 100;
    public static final String URI_LIST_NAME_PARTICIPANT = "participants";
    public static final String URI_LIST_NAME_COORDINATOR = "coordinator";

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
    protected BaseEventListener needDeactivator;
    protected BaseEventListener workDoneSignaller;

    @Override
    protected void initializeEventListeners()
    {
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();

        //create needs every trigger execution until NO_OF_NEEDS-1 needs are created
        this.participantNeedCreator = new ExecuteOnEventListener(
                ctx,
                new EventBotActions.CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_PARTICIPANT, FacetType.BACCParticipantFacet.getURI()),
                NO_OF_NEEDS - 1
        );
        bus.subscribe(ActEvent.class,this.participantNeedCreator);
        //create needs every trigger execution until coordinator need is created
        this.coordinatorNeedCreator = new ExecuteOnEventListener(
                ctx,
                new EventBotActions.CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_COORDINATOR, FacetType.BAAtomicCCCoordinatorFacet.getURI()),
                1
        );
        bus.subscribe(ActEvent.class,this.coordinatorNeedCreator);
        //count until NO_OF_NEEDS were created, then
        //   * connect the Coordinator with Participant needs
        this.needConnector = new ExecuteOnceAfterNEventsListener(ctx,
                new EventBotActions.ConnectFromListToListAction(
                        ctx, URI_LIST_NAME_COORDINATOR, URI_LIST_NAME_PARTICIPANT, FacetType.BAAtomicCCCoordinatorFacet.getURI(), FacetType.BACCParticipantFacet.getURI()),NO_OF_NEEDS);
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
        List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(NO_OF_NEEDS-1);

//        ACTIVE -> Exit
//        scripts.add(new BAAtomicCCStateActiveExiting());
//        scripts.add(new BAAtomicCCStateCompleted());
//        scripts.add(new BAAtomicCCStateCompleting());
//        scripts.add(new BAAtomicCCStateActive());

//        ACTIVE -> Fail
//        scripts.add(new BAAtomicCCStateActiveFailing());
//        scripts.add(new BAAtomicCCStateCompleted());
//        scripts.add(new BAAtomicCCStateCompleting());
//        scripts.add(new BAAtomicCCStateActive());

//         ACTIVE -> NotCompleting
//        scripts.add(new BAAtomicCCStateActiveNotCompleting());
//        scripts.add(new BAAtomicCCStateCompleted());
//        scripts.add(new BAAtomicCCStateCompleting());
//        scripts.add(new BAAtomicCCStateActive());


        this.autoResponder = new AutomaticBAMessageResponderListener(ctx, scripts, NO_OF_MESSAGES, MILLIS_BETWEEN_MESSAGES);
        bus.subscribe(OpenFromOtherNeedEvent.class, this.autoResponder);
        bus.subscribe(MessageFromOtherNeedEvent.class, this.autoResponder);

        //register a DeactivateAllNeedsAction to be executed
        //when the internalWorkDoneEvent is seen
        this.needDeactivator = new ExecuteOnEventListener(getEventListenerContext(), new
                EventBotActions.DeactivateAllNeedsAction(getEventListenerContext()),1);
        bus.subscribe(InternalWorkDoneEvent.class, needDeactivator);

        //add a listener that counts two NeedDeactivatedEvents and then tells the
        //framework that the bot's work is done
        this.workDoneSignaller = new ExecuteOnceAfterNEventsListener(
                ctx,
                new EventBotActions.SignalWorkDoneAction(ctx), NO_OF_NEEDS
        );
        bus.subscribe(NeedDeactivatedEvent.class, this.workDoneSignaller);
    }
}

