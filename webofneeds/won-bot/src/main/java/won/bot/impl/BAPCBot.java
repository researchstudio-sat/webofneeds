package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.event.*;
import won.bot.framework.events.listener.*;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

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

    //we use protected members so we can extend the class and
    //access the listeners for unit test assertions and stats
    //
    //we use BaseEventListener as their types so we can access the generic
    //functionality offered by that class
    protected BaseEventListener needCreator;
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

        //create needs every trigger execution until 2 needs are created
        this.needCreator = new ExecuteOnEventListener(
                ctx,
                new EventBotActions.CreateNeedAction(ctx),
                NO_OF_NEEDS
        );
        bus.subscribe(ActEvent.class,this.needCreator);

        //count until NO_OF_NEEDS were created, then
        //   * connect the Cooridnator with Participants needs
        this.needConnector = new ExecuteOnceAfterNEventsListener(ctx,
                new EventBotActions.ConnectBANeedsAction(
                        ctx, NO_OF_NEEDS, FacetType.BAPCParticipantFacet.getURI(), FacetType.BAPCCoordinatorFacet.getURI()), NO_OF_NEEDS);
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
        this.autoResponder = new AutomaticMessageResponderListener(ctx, NO_OF_MESSAGES, MILLIS_BETWEEN_MESSAGES);
        bus.subscribe(OpenFromOtherNeedEvent.class, this.autoResponder);
        bus.subscribe(BAStateChangeEvent.class, this.autoResponder);

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




















    @Override
    protected void initializeEventListeners()
    {
        //create needs every trigger execution until 2 needs are created
        getEventBus().subscribe(ActEvent.class, new CreateNeedOnActListener(getEventListenerContext(), NO_OF_NEEDS));

        //count until 2 needs were created, then
        //   * connect the 2 needs
        getEventBus().subscribe(NeedCreatedEvent.class, new ExecuteOnceAfterNEventsListener(getEventListenerContext(), NO_OF_NEEDS, new Runnable(){
            @Override
            public void run()
            {
                List<URI> needs = getBotContext().listNeedUris();
                try {
                    for(int i=1; i<NO_OF_NEEDS; i++)
                    {
                        getOwnerService().connect(needs.get(0), needs.get(i), WonRdfUtils.FacetUtils.createModelForConnect(FacetType.BAPCCoordinatorFacet.getURI(), FacetType.BAPCParticipantFacet.getURI()));
                    }
                } catch (Exception e) {
                    logger.warn("could not connect {} and {}", new Object[]{needs.get(0), needs.get(1)},e);
                }
            }
        }));

        //add a listener that is informed of the connect/open events and that auto-opens
        //subscribe it to:
        // * connect events - so it responds with open
        // * open events - so it responds with open (if the open received was the first open, and we still need to accept the connection)
        AutomaticConnectionOpenerListener autoOpener = new AutomaticConnectionOpenerListener(getEventListenerContext());
        getEventBus().subscribe(OpenFromOtherNeedEvent.class, autoOpener);
        getEventBus().subscribe(ConnectFromOtherNeedEvent.class, autoOpener);

        //add a listener that auto-responds to messages by a message
        //after 10 messages, it closes the connections
        //subscribe it to:
        // * message events - so it responds
        // * open events - so it initiates the chain reaction of responses
        BAPCMessageListener autoMessageResponder = new BAPCMessageListener(getEventListenerContext(), NO_OF_MESSAGES, MILLIS_BETWEEN_MESSAGES);
        getEventBus().subscribe(OpenFromOtherNeedEvent.class, autoMessageResponder);
        getEventBus().subscribe(MessageFromOtherNeedEvent.class, autoMessageResponder);

        //add a listener that auto-responds to a close message with a deactivation of both needs.
        //subscribe it to:
        // * close events
        DeactivateNeedsOnConnectionCloseListener needDeactivator= new DeactivateNeedsOnConnectionCloseListener(getEventListenerContext());
        getEventBus().subscribe(CloseFromOtherNeedEvent.class, needDeactivator);


    }

}
