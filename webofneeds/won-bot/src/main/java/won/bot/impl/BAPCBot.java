package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.event.*;
import won.bot.framework.events.listener.*;
import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.*;
import won.bot.framework.events.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.*;
import won.protocol.model.FacetType;

import java.util.ArrayList;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 12.2.14.
 * Time: 20.45
 * To change this template use File | Settings | File Templates.
 */
public class BAPCBot extends EventBot {
    private static final int NO_OF_NEEDS = 19;
    private static final int NO_OF_MESSAGES = 50;
    private static final long MILLIS_BETWEEN_MESSAGES = 1000;
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
        List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(NO_OF_NEEDS-1);

        //Coordination message is sent as TEXT
        scripts.add(new BAPCStateExitBot());
        scripts.add(new BAPCStateCompleteBot());
        scripts.add(new BAPCStateCompensateBot());
        scripts.add(new BAPCStateCompensateFailBot());
        scripts.add(new BAPCStateActiveFailBot());
        scripts.add(new BAPCStateActiveCancelBot());
        scripts.add(new BAPCStateActiveCancelFailBot());
        scripts.add(new BAPCStateActiveCannotCompleteBot());

        //Coordination message is sent as MODEL
        scripts.add(new BAPCStateExitUriBot());
        scripts.add(new BAPCStateCompleteUriBot());
        scripts.add(new BAPCStateCompensateUriBot());
        scripts.add(new BAPCStateCompensateFailUriBot());
        scripts.add(new BAPCStateActiveFailUriBot());
        scripts.add(new BAPCStateActiveCancelUriBot());
        scripts.add(new BAPCStateActiveCancelFailUriBot());
        scripts.add(new BAPCStateActiveCannotCompleteUriBot());

        //with failures
        scripts.add(new BAPCStateCompleteWithFailureUriBot());
        scripts.add(new BAPCStateCompleteWithFailureBot());

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

