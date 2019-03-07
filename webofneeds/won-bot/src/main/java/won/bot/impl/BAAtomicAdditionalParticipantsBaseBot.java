package won.bot.impl;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.AdditionalParticipantCoordinatorBotContextWrapper;
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
import won.bot.framework.eventbot.listener.impl.WaitForNEventsListener;
import won.protocol.model.FacetType;

/**
 * User: Danijel
 * Date: 7.5.14.
 */
public abstract class BAAtomicAdditionalParticipantsBaseBot extends EventBot{
  protected final int noOfNeeds;
  protected final int noOfDelayedNeeds;
  protected final int noOfNonDelayedNeeds;
  protected final List<BATestBotScript> firstPhaseScripts;
  protected final List<BATestBotScript> firstPhaseScriptsWithDelay;
  protected final List<BATestBotScript> secondPhaseScripts;
//  protected final List<BATestBotScript> secondPhaseScriptsWithDelay;
  private static final long MILLIS_BETWEEN_MESSAGES = 10;

  protected BaseEventListener participantNeedCreator;
  protected BaseEventListener delayedParticipantNeedCreator;
  protected BaseEventListener coordinatorNeedCreator;
  protected BaseEventListener needConnector;
  protected BaseEventListener needConnectorWithDelay;
  protected BaseEventListener scriptsDoneListener;
  protected BaseEventListener firstPhaseWithDelayDoneListener;
  protected BaseEventListener workDoneSignaller;
  protected final List<BATestScriptListener> firstPhasetestScriptListeners;
  protected final List<BATestScriptListener> firstPhasetestScriptWithDelayListeners;
  protected final List<BATestScriptListener> secondPhasetestScriptListeners;
 // protected final List<BATestScriptListener> secondPhasetestScriptWithDelayListeners;

  protected BaseEventListener firstPhaseCompleteListener;

  protected BAAtomicAdditionalParticipantsBaseBot() {
    this.firstPhaseScripts = getFirstPhaseScripts();
    this.firstPhaseScriptsWithDelay = getFirstPhaseScriptsWithDelay();
    this.secondPhaseScripts = getSecondPhaseScripts();
//    this.secondPhaseScriptsWithDelay = getSecondPhaseScriptsWithDelay();
//    if (this.secondPhaseScripts.size() != this.firstPhaseScripts.size()) {
//      throw new IllegalArgumentException("The same number of scripts in first and second phase is required!");
//    }
//    if (this.secondPhaseScriptsWithDelay.size() != this.firstPhaseScriptsWithDelay.size())
//      throw new IllegalArgumentException("The same number of scripts in first and second phase (wiht delay) is " +
//        "required!");

    this.noOfNonDelayedNeeds = firstPhaseScripts.size()+1;
    this.noOfDelayedNeeds = firstPhaseScriptsWithDelay.size();
    this.noOfNeeds = secondPhaseScripts.size()+1;
    this.firstPhasetestScriptListeners = Collections.synchronizedList(new ArrayList<BATestScriptListener>
    (noOfNonDelayedNeeds-1));
    this.firstPhasetestScriptWithDelayListeners = Collections.synchronizedList(new ArrayList<BATestScriptListener>
      (noOfDelayedNeeds));
    this.secondPhasetestScriptListeners = Collections.synchronizedList(new ArrayList<BATestScriptListener>
      (noOfNeeds-1));
 //   this.secondPhasetestScriptWithDelayListeners = new ArrayList<BATestScriptListener>(noOfDelayedNeeds);
  }

  protected abstract FacetType getParticipantFacetType();
  protected abstract FacetType getCoordinatorFacetType();

  @Override
  protected void initializeEventListeners() {
    final EventListenerContext ctx = getEventListenerContext();
    final AdditionalParticipantCoordinatorBotContextWrapper botContextWrapper = (AdditionalParticipantCoordinatorBotContextWrapper) getBotContextWrapper();
    final EventBus bus = getEventBus();
    logger.info("info1: No of needs: "+noOfNeeds);

    //wait for all needs to be created
    WaitForNEventsListener allNeedsCreatedListener = new WaitForNEventsListener(ctx,
      "waitForAllNeedsCreated",noOfNeeds);
    bus.subscribe(NeedCreatedEvent.class, allNeedsCreatedListener);


    //create needs every trigger execution until noOfNeeds are created
    this.participantNeedCreator = new ActionOnEventListener(
      ctx, "participantCreator",
      new CreateNeedWithFacetsAction(ctx, botContextWrapper.getParticipantListName(), getParticipantFacetType().getURI()),
      noOfNonDelayedNeeds - 1
    );
    bus.subscribe(ActEvent.class, this.participantNeedCreator);

    //create needs every trigger execution until noOfNeeds are created
    this.delayedParticipantNeedCreator = new ActionOnEventListener(
      ctx, "delayedParticipantCreator",
      new CreateNeedWithFacetsAction(ctx, botContextWrapper.getParticipantDelayedListName(), getParticipantFacetType().getURI()),
      noOfDelayedNeeds
    );
    bus.subscribe(ActEvent.class, this.delayedParticipantNeedCreator);


    //when done, create one coordinator need
    this.coordinatorNeedCreator = new ActionOnEventListener(
      ctx, "coordinatorCreator", new FinishedEventFilter(participantNeedCreator),
      new CreateNeedWithFacetsAction(ctx, botContextWrapper.getCoordinatorListName(), getCoordinatorFacetType().getURI()),
      1
    );
    bus.subscribe(FinishedEvent.class, this.coordinatorNeedCreator);

    final Iterator<BATestBotScript> firstPhasescriptIterator = firstPhaseScripts.iterator();
    final Iterator<BATestBotScript> firstPhaseScriptWithDelayIterator = firstPhaseScriptsWithDelay.iterator();
    final Iterator<BATestBotScript> secondPhasescriptIterator = secondPhaseScripts.iterator();
 //   final Iterator<BATestBotScript> secondPhasescriptWithDelayIterator = secondPhaseScripts.iterator();

    //make a composite filter, with one filter for each testScriptListener that wait
    // for the FinishedEvents the they emit. That filter will be used to shut
    // down all needs after all the scriptListeners have finished.
    final OrFilter firstPhaseScriptListenerFilter = new OrFilter();
    final OrFilter firstPhaseScriptWithDelayListenerFilter = new OrFilter();
    final OrFilter secondPhaseScriptListenerFilter = new OrFilter();
 //   final OrFilter secondPhaseScriptWithDelayListenerFilter = new OrFilter();
    //create a callback that gets called immediately before the connection is established
    ConnectFromListToListAction.ConnectHook scriptConnectHook = new ConnectFromListToListAction.ConnectHook()
    {
      @Override
      public void onConnect(final URI fromNeedURI, final URI toNeedURI) {
        //create the listener that will execute the script actions
        BATestScriptListener testScriptListener = new BATestScriptListener(ctx, firstPhasescriptIterator.next(), fromNeedURI,
          toNeedURI, MILLIS_BETWEEN_MESSAGES);
        //remember it so we can check its state later
        firstPhasetestScriptListeners.add(testScriptListener);

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
          secondPhasescriptIterator.next(),fromNeedURI,toNeedURI, MILLIS_BETWEEN_MESSAGES);
        secondPhasetestScriptListeners.add(secondPhaseTestScriptListener);
        secondPhaseScriptListenerFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(secondPhaseTestScriptListener)));
      }
    };


    ConnectFromListToListAction.ConnectHook scriptConnectWithDelayHook = new ConnectFromListToListAction.ConnectHook()
    {
      @Override
      public void onConnect(final URI fromNeedURI, final URI toNeedURI) {
        //create the listener that will execute the script actions
        BATestScriptListener testScriptListener = new BATestScriptListener(ctx, firstPhaseScriptWithDelayIterator.next(),
          fromNeedURI, toNeedURI, MILLIS_BETWEEN_MESSAGES);
        //remember it so we can check its state later
        firstPhasetestScriptWithDelayListeners.add(testScriptListener);

        //subscribe it to the relevant events.
        bus.subscribe(ConnectFromOtherNeedEvent.class, testScriptListener);
        bus.subscribe(OpenFromOtherNeedEvent.class, testScriptListener);
        bus.subscribe(MessageFromOtherNeedEvent.class, testScriptListener);
        //add a filter that will wait for the FinishedEvent emitted by that listener
        //wrap it in an acceptance filter to make extra sure we count each listener only once.
        firstPhaseScriptWithDelayListenerFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(testScriptListener)));

        //now we create the listener that is only active in the second phase
        //remember it so we can check its state later
        BATestScriptListener secondPhaseTestScriptListener = new BATestScriptListener(ctx,
          secondPhasescriptIterator.next(),fromNeedURI,toNeedURI, MILLIS_BETWEEN_MESSAGES);
          secondPhasetestScriptListeners.add(secondPhaseTestScriptListener);
          secondPhaseScriptListenerFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(secondPhaseTestScriptListener)));
      }
    };


    //when done, connect the participants to the coordinator
    this.needConnector = new ActionOnEventListener(
      ctx, "needConnector", new FinishedEventFilter(allNeedsCreatedListener),
      new ConnectFromListToListAction(ctx, botContextWrapper.getCoordinatorListName(), botContextWrapper.getParticipantListName(),
        getCoordinatorFacetType().getURI(),
        getParticipantFacetType().getURI(), MILLIS_BETWEEN_MESSAGES,
        scriptConnectHook, "Hi!"),1);
    bus.subscribe(FinishedEvent.class, this.needConnector);

    //wait until the non-delayed participants are connected and done with their scripts
    BaseEventListener waitForNonDelayedConnectsListener = new WaitForNEventsListener(ctx,
      firstPhaseScriptListenerFilter, noOfNonDelayedNeeds - 1);
    bus.subscribe(FinishedEvent.class, waitForNonDelayedConnectsListener);

    FinishedEventFilter allNonDelayedConnectedFilter = new FinishedEventFilter(waitForNonDelayedConnectsListener);

    this.needConnectorWithDelay = new ActionOnEventListener(
      ctx, "needConnectorWithDelay", allNonDelayedConnectedFilter,
      new ConnectFromListToListAction(ctx, botContextWrapper.getCoordinatorListName(), botContextWrapper.getParticipantDelayedListName(),
        getCoordinatorFacetType().getURI(),
        getParticipantFacetType().getURI(), MILLIS_BETWEEN_MESSAGES,
        scriptConnectWithDelayHook, "Hi!"),1);
    bus.subscribe(FinishedEvent.class, this.needConnectorWithDelay); //TODO: MAKE THIS SO URI_LIST_NAME_PARTICIPANT_DELAYED "delayedParticipants" works again

    //for each group member, there are 2 listeners waiting for messages. when they are all finished, we're done.
    this.firstPhaseWithDelayDoneListener = new ActionOnceAfterNEventsListener(
      ctx, "firstPhaseDoneWithDelayListener", firstPhaseScriptWithDelayListenerFilter,
      noOfDelayedNeeds,
      new BaseEventBotAction(ctx)
      {
        @Override
        protected void doRun(final Event event, EventListener executingListener) throws Exception {
          logger.debug("starting second phase");
          logger.debug("non-delayed listeners: {}", firstPhasetestScriptListeners.size());
          logger.debug("delayed listeners: {}", firstPhasetestScriptWithDelayListeners.size());
          Iterator<BATestScriptListener> firstPhaseListeners = firstPhasetestScriptListeners.iterator();
          Iterator<BATestScriptListener>  firstPhaseWithDelayListeners = firstPhasetestScriptWithDelayListeners
            .iterator();
          Iterator<BATestScriptListener>  combinedFirstPhaseListeners = Iterators.concat(firstPhaseListeners,
            firstPhaseWithDelayListeners);
          logger.debug("# of listeners in second phase: {}", secondPhasetestScriptListeners.size());
          for(BATestScriptListener listener: secondPhasetestScriptListeners){
            logger.debug("subscribing second phase listener {}", listener);
            //subscribe it to the relevant events.
            bus.subscribe(MessageFromOtherNeedEvent.class, listener);
            bus.subscribe(SecondPhaseStartedEvent.class, listener);
            BATestScriptListener correspondingFirstPhaseListener = combinedFirstPhaseListeners.next();
            listener.setCoordinatorSideConnectionURI(correspondingFirstPhaseListener.getCoordinatorSideConnectionURI());
            listener.setParticipantSideConnectionURI(correspondingFirstPhaseListener.getParticipantSideConnectionURI());
            listener.updateFilterForBothConnectionURIs();
            bus.publish(new SecondPhaseStartedEvent(
              correspondingFirstPhaseListener.getCoordinatorURI(),
              correspondingFirstPhaseListener.getCoordinatorSideConnectionURI(),
              correspondingFirstPhaseListener.getParticipantURI()));
          }
        }
      });
    bus.subscribe(FinishedEvent.class, this.firstPhaseWithDelayDoneListener);


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
  protected abstract List<BATestBotScript> getFirstPhaseScriptsWithDelay();

}
