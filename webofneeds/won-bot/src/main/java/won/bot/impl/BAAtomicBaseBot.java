package won.bot.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.ParticipantCoordinatorBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.lifecycle.SignalWorkDoneAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.CreateNeedWithFacetsAction;
import won.bot.framework.eventbot.action.impl.needlifecycle.DeactivateAllNeedsOfListAction;
import won.bot.framework.eventbot.action.impl.wonmessage.ConnectFromListToListAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.listener.FinishedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.impl.AcceptOnceFilter;
import won.bot.framework.eventbot.filter.impl.FinishedEventFilter;
import won.bot.framework.eventbot.filter.impl.OrFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptListener;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.SecondPhaseStartedEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.protocol.model.FacetType;

/**
 * User: Danijel
 * Date: 10.4.14.
 */
public abstract class BAAtomicBaseBot extends EventBot{
  protected final int noOfNeeds;
  protected final List<TwoPhaseScript> scripts;
  private static final long MILLIS_BETWEEN_MESSAGES = 10;

  protected BaseEventListener participantNeedCreator;
  protected BaseEventListener coordinatorNeedCreator;
  protected BaseEventListener needConnector;
  protected BaseEventListener scriptsDoneListener;
  protected BaseEventListener firstPhaseDoneListener;
  protected BaseEventListener workDoneSignaller;
  protected final List<TwoPhaseScriptListener> scriptListeners;

  protected BaseEventListener firstPhaseCompleteListener;
  private Object scriptIteratorMonitor = new Object();

  protected BAAtomicBaseBot() {
    this.scripts = setupScripts();
    this.noOfNeeds = scripts.size()+1;
    this.scriptListeners = Collections.synchronizedList( new ArrayList <TwoPhaseScriptListener>(noOfNeeds
      -1));
  }

  /**
   * Fetches scripts for first and second phase from concrete implementation and builds one list of TwoPhaseScript
   * objects.
   * @return
   */
  private List<TwoPhaseScript> setupScripts(){
    List<BATestBotScript> firstPhaseScripts = getFirstPhaseScripts();
    List<BATestBotScript> secondPhaseScripts = getSecondPhaseScripts();
    if (secondPhaseScripts.size() != firstPhaseScripts.size()) {
      throw new IllegalArgumentException("The same number of scripts in first and second phase is required!");
    }
    List<TwoPhaseScript> scripts = new LinkedList<TwoPhaseScript>();
    Iterator<BATestBotScript> firstIter = firstPhaseScripts.iterator();
    Iterator<BATestBotScript> secondIter = secondPhaseScripts.iterator();
    while(firstIter.hasNext()){
      TwoPhaseScript script = new TwoPhaseScript(firstIter.next(), secondIter.next());
      scripts.add(script);
    }
    return Collections.synchronizedList(scripts);
  }

  protected abstract FacetType getParticipantFacetType();
  protected abstract FacetType getCoordinatorFacetType();

  @Override
  protected void initializeEventListeners() {
    final EventListenerContext ctx = getEventListenerContext();
    final EventBus bus = getEventBus();

    ParticipantCoordinatorBotContextWrapper botContextWrapper = (ParticipantCoordinatorBotContextWrapper) getBotContextWrapper();

    //create needs every trigger execution until noOfNeeds are created
    this.participantNeedCreator = new ActionOnEventListener(
      ctx, "participantCreator",
      new CreateNeedWithFacetsAction(ctx, botContextWrapper.getParticipantListName(), getParticipantFacetType().getURI()),
      noOfNeeds - 1
    );
    bus.subscribe(ActEvent.class, this.participantNeedCreator);

    //when done, create one coordinator need
    this.coordinatorNeedCreator = new ActionOnEventListener(
      ctx, "coordinatorCreator", new FinishedEventFilter(participantNeedCreator),
      new CreateNeedWithFacetsAction(ctx, botContextWrapper.getCoordinatorListName(), getCoordinatorFacetType().getURI()),
      1
    );
    bus.subscribe(FinishedEvent.class, this.coordinatorNeedCreator);
    FinishedEventFilter coordinatorCreatorFilter = new FinishedEventFilter(coordinatorNeedCreator);

    final Iterator<TwoPhaseScript> scriptIterator = scripts.iterator();

    //make a composite filter, with one filter for each testScriptListener that wait
    // for the FinishedEvents the they emit. That filter will be used to shut
    // down all needs after all the scriptListeners have finished.
    final OrFilter firstPhaseScriptListenerFilter = new OrFilter();
    final OrFilter secondPhaseScriptListenerFilter = new OrFilter();
    //create a callback that gets called immediately before the connection is established
    ConnectFromListToListAction.ConnectHook scriptConnectHook = new ConnectFromListToListAction.ConnectHook()
    {
      @Override
      public void onConnect(final URI fromNeedURI, final URI toNeedURI) {
        TwoPhaseScript script = scriptIterator.next();

        //create the listener that will execute the script actions
        BATestScriptListener testScriptListener = new BATestScriptListener(ctx, script.getFirstPhaseScript(), fromNeedURI,
                                                                           toNeedURI, MILLIS_BETWEEN_MESSAGES);
        //subscribe it to the relevant events.
        bus.subscribe(ConnectFromOtherNeedEvent.class, testScriptListener);
        bus.subscribe(OpenFromOtherNeedEvent.class, testScriptListener);
        bus.subscribe(MessageFromOtherNeedEvent.class, testScriptListener);
        //add a filter that will wait for the FinishedEvent emitted by that listener
        //wrap it in an acceptance filter to make extra sure we count each listener only once.
        firstPhaseScriptListenerFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(testScriptListener)));

        //now we create the listener that is only active in the second phase
        //remember it so we can check its state later
        BATestScriptListener secondPhaseTestScriptListener = new BATestScriptListener(ctx,
          script.getSecondPhaseScript(),fromNeedURI,toNeedURI, MILLIS_BETWEEN_MESSAGES);

        //remember both listeners as a pair as we'll need them together later
        TwoPhaseScriptListener twoPhaseScriptListener = new TwoPhaseScriptListener(testScriptListener,
          secondPhaseTestScriptListener);
        scriptListeners.add(twoPhaseScriptListener);

        secondPhaseScriptListenerFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(secondPhaseTestScriptListener)));
      }
    };

    //when done, connect the participants to the coordinator
    this.needConnector = new ActionOnceAfterNEventsListener(
      ctx, "needConnector", noOfNeeds,
      new ConnectFromListToListAction(ctx, botContextWrapper.getCoordinatorListName(), botContextWrapper.getParticipantListName(),
                                      getCoordinatorFacetType().getURI(),
                                      getParticipantFacetType().getURI(), MILLIS_BETWEEN_MESSAGES,
        scriptConnectHook,"Hi!"));
    bus.subscribe(NeedCreatedEvent.class, this.needConnector);
    //for each group member, there are 2 listeners waiting for messages. when they are all finished, we're done.
    this.firstPhaseDoneListener = new ActionOnceAfterNEventsListener(
      ctx, "firstPhaseDoneListener", firstPhaseScriptListenerFilter,
      noOfNeeds - 1,
      new BaseEventBotAction(ctx)
      {
        @Override
        protected void doRun(final Event event, EventListener executingListener) throws Exception {
          logger.debug("starting second phase");
          for(TwoPhaseScriptListener listener: scriptListeners){
            logger.debug("subscribing second phase listener {}", listener);
            //subscribe it to the relevant events.
            bus.subscribe(MessageFromOtherNeedEvent.class, listener.getSecondPhaseListener());
            bus.subscribe(SecondPhaseStartedEvent.class, listener.getSecondPhaseListener());

            listener.getSecondPhaseListener().setCoordinatorSideConnectionURI(listener.getFirstPhaseListener()
              .getCoordinatorSideConnectionURI());
            listener.getSecondPhaseListener().setParticipantSideConnectionURI(listener.getFirstPhaseListener()
              .getParticipantSideConnectionURI());
            listener.getSecondPhaseListener().updateFilterForBothConnectionURIs();
            bus.publish(new SecondPhaseStartedEvent(
              listener.getFirstPhaseListener().getCoordinatorURI(),
              listener.getFirstPhaseListener().getCoordinatorSideConnectionURI(),
              listener.getFirstPhaseListener().getParticipantURI()));
          }
        }
      });
    bus.subscribe(FinishedEvent.class, this.firstPhaseDoneListener);


    //for each group member, there are 2 listeners waiting for messages. when they are all finished, we're done.
    this.scriptsDoneListener = new ActionOnceAfterNEventsListener(
      ctx, "scriptsDoneListener", secondPhaseScriptListenerFilter,
      noOfNeeds - 1,
      new DeactivateAllNeedsOfListAction(ctx, botContextWrapper.getParticipantListName()));
      bus.subscribe(FinishedEvent.class, this.scriptsDoneListener);


    //When the needs are deactivated, all connections are closed. wait for the close events and signal work done.
    this.workDoneSignaller = new ActionOnceAfterNEventsListener(
      ctx, "workDoneSignaller",
      noOfNeeds - 1, new SignalWorkDoneAction(ctx)
    );
    bus.subscribe(CloseFromOtherNeedEvent.class, this.workDoneSignaller);

  }


  protected abstract List<BATestBotScript> getFirstPhaseScripts();
  protected abstract List<BATestBotScript> getSecondPhaseScripts();

  private class TwoPhaseScript{
    private BATestBotScript firstPhaseScript;
    private BATestBotScript secondPhaseScript;

    private TwoPhaseScript(final BATestBotScript firstPhaseScript, final BATestBotScript secondPhaseScript) {
      this.firstPhaseScript = firstPhaseScript;
      this.secondPhaseScript = secondPhaseScript;
    }

    public BATestBotScript getSecondPhaseScript() {
      return secondPhaseScript;
    }

    public BATestBotScript getFirstPhaseScript() {
      return firstPhaseScript;
    }
  }

  private class TwoPhaseScriptListener {
    private BATestScriptListener firstPhaseListener;
    private BATestScriptListener secondPhaseListener;

    private TwoPhaseScriptListener(final BATestScriptListener firstPhaseListener, final BATestScriptListener secondPhaseListener) {
      this.firstPhaseListener = firstPhaseListener;
      this.secondPhaseListener = secondPhaseListener;
    }

    public BATestScriptListener getFirstPhaseListener() {
      return firstPhaseListener;
    }

    public BATestScriptListener getSecondPhaseListener() {
      return secondPhaseListener;
    }
  }
}
