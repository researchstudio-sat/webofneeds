package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.impl.ConnectFromListToListAction;
import won.bot.framework.events.action.impl.CreateNeedWithFacetsAction;
import won.bot.framework.events.action.impl.DeactivateAllNeedsOfGroupAction;
import won.bot.framework.events.action.impl.SignalWorkDoneAction;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.impl.*;
import won.bot.framework.events.filter.impl.AcceptOnceFilter;
import won.bot.framework.events.filter.impl.FinishedEventFilter;
import won.bot.framework.events.filter.impl.OrFilter;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;
import won.bot.framework.events.listener.impl.ActionOnceAfterNEventsListener;
import won.protocol.model.FacetType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: Danijel
 * Date: 10.4.14.
 */
public abstract class BAAtomicBaseBot extends EventBot{
  public static final String URI_LIST_NAME_PARTICIPANT = "participants";
  public static final String URI_LIST_NAME_COORDINATOR = "coordinator";
  protected final int noOfNeeds;
  protected final List<BATestBotScript> scripts;
  private static final long MILLIS_BETWEEN_MESSAGES = 10;

  protected BaseEventListener participantNeedCreator;
  protected BaseEventListener coordinatorNeedCreator;
  protected BaseEventListener needConnector;
  protected BaseEventListener scriptsDoneListener;
  protected BaseEventListener workDoneSignaller;
  protected final List<BATestScriptListener> testScriptListeners;

  protected BaseEventListener firstPhaseCompleteListener;

  protected BAAtomicBaseBot() {
    this.scripts= getScripts();
    this.noOfNeeds = scripts.size();
    this.testScriptListeners = new ArrayList<BATestScriptListener>(noOfNeeds);
  }

  @Override
  protected void initializeEventListeners() {
    final EventListenerContext ctx = getEventListenerContext();
    final EventBus bus = getEventBus();

    //create needs every trigger execution until noOfNeeds are created
    this.participantNeedCreator = new ActionOnEventListener(
      ctx, "participantCreator",
      new CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_PARTICIPANT, FacetType.BACCParticipantFacet.getURI()),
      noOfNeeds - 1
    );
    bus.subscribe(ActEvent.class, this.participantNeedCreator);

    //when done, create one coordinator need
    this.coordinatorNeedCreator = new ActionOnEventListener(
      ctx, "coordinatorCreator", new FinishedEventFilter(participantNeedCreator),
      new CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_COORDINATOR, FacetType.BAAtomicCCCoordinatorFacet.getURI()),
      1
    );
    bus.subscribe(FinishedEvent.class, this.coordinatorNeedCreator);


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
        //wrap it in an acceptance filter to make extra sure we count each listener only once.
        mainScriptListenerFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(testScriptListener)));
      }
    };

    //when done, connect the participants to the coordinator
    this.needConnector = new ActionOnceAfterNEventsListener(
      ctx, "needConnector", noOfNeeds,
      new ConnectFromListToListAction(ctx, URI_LIST_NAME_COORDINATOR, URI_LIST_NAME_PARTICIPANT,
                                      FacetType.BACCCoordinatorFacet.getURI(), FacetType.BACCParticipantFacet.getURI(), MILLIS_BETWEEN_MESSAGES,
                                      scriptConnectHook));
    bus.subscribe(NeedCreatedEvent.class, this.needConnector);


    //for each group member, there are 2 listeners waiting for messages. when they are all finished, we're done.
    this.scriptsDoneListener = new ActionOnceAfterNEventsListener(
      ctx, "scriptsDoneListener", mainScriptListenerFilter,
      noOfNeeds - 1,
      new DeactivateAllNeedsOfGroupAction(ctx, URI_LIST_NAME_PARTICIPANT));
    bus.subscribe(FinishedEvent.class, this.scriptsDoneListener);

    //When the needs are deactivated, all connections are closed. wait for the close events and signal work done.
    this.workDoneSignaller = new ActionOnceAfterNEventsListener(
      ctx, "workDoneSignaller",
      noOfNeeds - 1, new SignalWorkDoneAction(ctx)
    );
    bus.subscribe(CloseFromOtherNeedEvent.class, this.workDoneSignaller);
  }


  protected abstract List<BATestBotScript> getScripts();
}
