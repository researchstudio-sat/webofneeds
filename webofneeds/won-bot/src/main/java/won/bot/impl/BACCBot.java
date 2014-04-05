package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.event.*;
import won.bot.framework.events.listener.*;
import won.bot.framework.events.listener.action.ConnectFromListToListAction;
import won.bot.framework.events.listener.action.CreateNeedWithFacetsAction;
import won.bot.framework.events.listener.action.DeactivateAllNeedsOfGroupAction;
import won.bot.framework.events.listener.action.SignalWorkDoneAction;
import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptListener;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.*;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.coordinationnMessageAsTextBots.*;
import won.bot.framework.events.listener.filter.AcceptOnceFilter;
import won.bot.framework.events.listener.filter.FinishedEventFilter;
import won.bot.framework.events.listener.filter.OrFilter;
import won.protocol.model.FacetType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 26.2.14.
 * Time: 15.15
 * To change this template use File | Settings | File Templates.
 */
public class BACCBot extends EventBot {
    protected static final int NO_OF_NEEDS = 29;
    protected static final int NO_OF_MESSAGES = 50;
    private static final long MILLIS_BETWEEN_MESSAGES = 10;
    public static final String URI_LIST_NAME_PARTICIPANT = "participants";
    public static final String URI_LIST_NAME_COORDINATOR = "coordinator";
    protected static final List<BATestBotScript> scripts = getScripts();

    //we use protected members so we can extend the class and
    //access the listeners for unit test assertions and stats
    //
    //we use BaseEventListener as their types so we can access the generic
    //functionality offered by that class
    protected BaseEventListener participantNeedCreator;
    protected BaseEventListener coordinatorNeedCreator;
    protected BaseEventListener needConnector;
    protected BaseEventListener scriptsDoneListener;
    protected BaseEventListener workDoneSignaller;
    protected List<BATestScriptListener> testScriptListeners = new ArrayList<BATestScriptListener>(NO_OF_NEEDS);

  @Override
    protected void initializeEventListeners()
    {
      final EventListenerContext ctx = getEventListenerContext();
      final EventBus bus = getEventBus();

      //create needs every trigger execution until NO_OF_NEEDS are created
      this.participantNeedCreator = new ExecuteOnEventListener(
        ctx, "participantCreator",
        new CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_PARTICIPANT, FacetType.BACCParticipantFacet.getURI()),
        NO_OF_NEEDS - 1
      );
      bus.subscribe(ActEvent.class, this.participantNeedCreator);

      //when done, create one coordinator need
      this.coordinatorNeedCreator = new ExecuteOnEventListener(
        ctx,"coordinatorCreator", new FinishedEventFilter(participantNeedCreator),
        new CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_COORDINATOR, FacetType.BACCCoordinatorFacet.getURI()),
        1
      );
      bus.subscribe(FinishedEvent.class,this.coordinatorNeedCreator);


      final Iterator<BATestBotScript> scriptIterator = scripts.iterator();
      //make a composite filter, with one filter for each testScriptListener that wait
      // for the FinishedEvents the they emit. That filter will be used to shut
      // down all needs after all the scriptListeners have finished.
      final OrFilter mainScriptListenerFilter = new OrFilter();
      //create a callback that gets called immediately before the connection is established
      ConnectFromListToListAction.ConnectHook scriptConnectHook = new ConnectFromListToListAction.ConnectHook()
      {
        @Override
        public void onConnect(final URI fromNeedURI, final URI toNeedURI) {
          //create the listener that will execute the script actions
          BATestScriptListener testScriptListener = new BATestScriptListener(ctx, scriptIterator.next(), fromNeedURI,
           toNeedURI, MILLIS_BETWEEN_MESSAGES);
          //remember it so we can check its state later
          testScriptListeners.add(testScriptListener);
          //subscribe it to the relevant events.
          bus.subscribe(ConnectFromOtherNeedEvent.class, testScriptListener);
          bus.subscribe(OpenFromOtherNeedEvent.class, testScriptListener);
          bus.subscribe(MessageFromOtherNeedEvent.class, testScriptListener);
          //add a filter that will wait for the FinishedEvent emitted by that listener
          //wrap it in an acceptonce filter to make extra sure we count each listener only once.
          mainScriptListenerFilter.addFilter(
            new AcceptOnceFilter(
              new FinishedEventFilter(testScriptListener)));
        }
      };

      //when done, connect the participants to the coordinator
      this.needConnector = new ExecuteOnceAfterNEventsListener(
        ctx, "needConnector", NO_OF_NEEDS,
        new ConnectFromListToListAction(ctx, URI_LIST_NAME_COORDINATOR, URI_LIST_NAME_PARTICIPANT,
          FacetType.BACCCoordinatorFacet.getURI(), FacetType.BACCParticipantFacet.getURI(), MILLIS_BETWEEN_MESSAGES,
          scriptConnectHook));
      bus.subscribe(NeedCreatedEvent.class, this.needConnector);


      //for each group member, there are 2 listeners waiting for messages. when they are all finished, we're done.
      this.scriptsDoneListener = new ExecuteOnceAfterNEventsListener(
        ctx, "scriptsDoneListener", mainScriptListenerFilter,
        NO_OF_NEEDS -1,
        new DeactivateAllNeedsOfGroupAction(ctx, URI_LIST_NAME_PARTICIPANT));
      bus.subscribe(FinishedEvent.class, this.scriptsDoneListener);

      //When the needs are deactivated, all connections are closed. wait for the close events and signal work done.
      this.workDoneSignaller = new ExecuteOnceAfterNEventsListener(
        ctx, "workDoneSignaller",
        NO_OF_NEEDS-1, new SignalWorkDoneAction(ctx)
      );
      bus.subscribe(CloseFromOtherNeedEvent.class, this.workDoneSignaller);
    }

  private static List<BATestBotScript> getScripts(){
    //add a listener that auto-responds to messages by a message
    //after NO_OF_MESSAGES messages, it unsubscribes from all events
    //subscribe it to:
    // * message events - so it responds
    // * open events - so it initiates the chain reaction of responses
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(NO_OF_NEEDS-1);

    //Coordination message is sent as TEXT
    scripts.add(new BACCStateExitBot());
    scripts.add(new BACCStateCompensateBot());
    scripts.add(new BACCStateCompleteBot());
    scripts.add(new BACCStateCompensateFailBot());
    scripts.add(new BACCStateCompleteFailBot());
    scripts.add(new BACCStateCompleteCancelBot());
    scripts.add(new BACCStateCompleteCancelFailBot());
    scripts.add(new BACCStateActiveCancelBot());
    scripts.add(new BACCStateActiveCancelFailBot());
    scripts.add(new BACCStateCompleteExitBot());
    scripts.add(new BACCStateActiveCannotCompleteBot());
    scripts.add(new BACCStateActiveFailBot());
    scripts.add(new BACCStateCompleteCannotCompleteBot());


    //Coordination message is sent as MODEL
    scripts.add(new BACCStateExitUriBot());
    scripts.add(new BACCStateCompensateUriBot());
    scripts.add(new BACCStateCompleteUriBot());
    scripts.add(new BACCStateCompensateFailUriBot());
    scripts.add(new BACCStateCompleteFailUriBot());
    scripts.add(new BACCStateCompleteCancelUriBot());
    scripts.add(new BACCStateCompleteCancelFailUriBot());
    scripts.add(new BACCStateActiveCancelUriBot());
    scripts.add(new BACCStateActiveCancelFailUriBot());
    scripts.add(new BACCStateCompleteExitUriBot());
    scripts.add(new BACCStateActiveCannotCompleteUriBot());
    scripts.add(new BACCStateActiveFailUriBot());
    scripts.add(new BACCStateCompleteCannotCompleteUriBot());


    // with failures
    scripts.add(new BACCStateCompleteWithFailuresUriBot());
    scripts.add(new BACCStateCompleteWithFailuresBot());
    return scripts;
  }
}

