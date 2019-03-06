/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.framework.eventbot.listener.baStateBots;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.NeedSpecificEvent;
import won.bot.framework.eventbot.event.RemoteNeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.filter.impl.AndFilter;
import won.bot.framework.eventbot.filter.impl.ConnectionUriEventFilter;
import won.bot.framework.eventbot.filter.impl.NeedUriEventFilter;
import won.bot.framework.eventbot.filter.impl.NotFilter;
import won.bot.framework.eventbot.filter.impl.OrFilter;
import won.bot.framework.eventbot.listener.AbstractFinishingListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Listener used to execute a business activity test script. It knows the URIs of one participant and one
 * coordinator and sends messages on behalf of these two as defined by the script it is given.
 *
 * It expects other listeners to send connect messages for the needs it controls.
 */
public class BATestScriptListener extends AbstractFinishingListener
{
  private BATestBotScript script;
  private URI coordinatorURI;
  private URI participantURI;
  private URI coordinatorSideConnectionURI = null;
  private URI participantSideConnectionURI = null;
  private int messagesInFlight = 0;
  private final Object countMonitor = new Object();
  private final Object filterChangeMonitor = new Object();
  private long millisBetweenMessages = 10;

  public BATestScriptListener(final EventListenerContext context, final BATestBotScript script,
    final URI coordinatorURI, final URI participantURI, long millisBetweenMessages) {
    super(context, createEventFilter(coordinatorURI, participantURI));
    this.script = script;
    this.coordinatorURI = coordinatorURI;
    this.participantURI = participantURI;
    this.millisBetweenMessages = millisBetweenMessages;
    this.name=script.getName();
  }

  public BATestScriptListener(final EventListenerContext context, final String name, final BATestBotScript script,
    final URI coordinatorURI, final URI participantURI, long millisBetweenMessages) {
    super(context, name, createEventFilter(coordinatorURI, participantURI));
    this.script = script;
    this.coordinatorURI = coordinatorURI;
    this.participantURI = participantURI;
    this.millisBetweenMessages = millisBetweenMessages;
  }

  protected static EventFilter createEventFilter(URI coordinatorURI, URI participantURI) {
    AndFilter mainFilter = new AndFilter();
    OrFilter orFilter = new OrFilter();
    orFilter.addFilter(new NeedUriEventFilter(coordinatorURI));
    orFilter.addFilter(new NeedUriEventFilter(participantURI));
    mainFilter.addFilter(orFilter);
    return mainFilter;
  }

  @Override
  public boolean isFinished() {
    synchronized (countMonitor) {
      //messagesInFlight may become negative if a message is received that we didn't send.
      boolean bFinished =(!script.hasNext()) && messagesInFlight <= 0;
      logger.debug("isFinished()=={}, scripts.hasNext()=={}, messagesInFlight =={}", new Object[]{bFinished,
        script.hasNext(),
        messagesInFlight});
      return bFinished;
    }
  }

  @Override
  protected void unsubscribe() {
    getEventListenerContext().getEventBus().unsubscribe(this);
  }

  @Override
  protected void handleEvent(final Event event) throws Exception {
    if (!(event instanceof NeedSpecificEvent && event instanceof ConnectionSpecificEvent)) {
      return;
    }
    logger.debug("handling event: {}", event);
    //extract need URI and connection URI from event
    URI needURI = ((NeedSpecificEvent) event).getNeedURI();
    URI connectionURI = ((ConnectionSpecificEvent) event).getConnectionURI();
    //at the beginning, we don't know the connection URIs - they are generated by the connect call
    //so we have to check if the event we're seeing is really about the connection we're interested in
    //it could be about another connection of one of the two needs.
    synchronized (filterChangeMonitor) {
      if (!bothConnectionURIsAreKnown()) {
        if (! isConnectionURIKnown(connectionURI)) {
          //we haven't checked the connectionURI before
          //we have to check if the connectionURI is relevant
          if (isRelevantEvent(event, needURI, connectionURI)) {
            //the connectionURI is relevant. remember the connect
            rememberConnectionURI(needURI, connectionURI);
            if (bothConnectionURIsAreKnown()){
              updateFilterForBothConnectionURIs();
            }
          } else {
            addIrrelevantConnectionURIToFilter(connectionURI);
            logger.debug("omitting event {} as it is not relevant for listener {}", event, this);
            return;
          }
        }
      } else if (!isConnectionURIKnown(connectionURI)){
        logger.debug("omitting event {} as it is not relevant for listener {}", event, this);
        return;
      }
    }

    if (event instanceof ConnectFromOtherNeedEvent){
      //send an automatic open
      logger.debug("sending automatic open in response to connect");
      sendOpen(connectionURI, new Date(System.currentTimeMillis() + millisBetweenMessages));
      synchronized (countMonitor){
        this.messagesInFlight++;
      }
      return;
    }

    // execute the next script action
    if (this.script.hasNext()){
      //if there is an action, execute it.
      BATestScriptAction action = this.script.getNextAction();
      logger.debug("executing next script action: {}", action);
      if (action.isNopAction()){
        logger.debug("not sending any messages for action {}", action);
        //if there are no more messages in the script, we're done:
        // it means we don't expect to receive more messages from anyone else, and
        // because we don't send one, we won't receive one from ourselves
        if (!script.hasNext()){
          logger.debug("unsubscribing from all events as last script action is NOP");
          performFinish();
        }
      } else {
        URI fromCon = getConnectionToSendFrom(action.isSenderIsCoordinator());
        URI fromNeed = getNeedToSendFrom(action.isSenderIsCoordinator());
        URI toCon = getConnectionToSendFrom(!action.isSenderIsCoordinator());
        URI toNeed = getNeedToSendFrom(!action.isSenderIsCoordinator());

        logger.debug("sending message for action {} on connection {}", action, fromCon);
        assertCorrectConnectionState(fromCon, action);
        sendMessage(action, fromCon, fromNeed, toCon, toNeed, new Date(System.currentTimeMillis() +
                                                                      millisBetweenMessages));
        synchronized (countMonitor){
          this.messagesInFlight++;
        }
      }
    } else {
      logger.debug("script has no more actions.");
    }
    //in any case: remember that we processed a message. Especially important for the message sent
    //through the last action, which we have to process as well otherwise the listener will finish too early
    //which may cause the bot to finish and the whole application to shut down before all messages have been
    //received, which leads to ugly exceptions
    if (event instanceof MessageEvent){
      //only decrement the message counter if the event indicates that
      //we received a message
      synchronized (countMonitor){
        this.messagesInFlight--;
      }
    }
  }

  private void assertCorrectConnectionState(final URI fromCon, final BATestScriptAction action) {

    LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();
    if (linkedDataSource instanceof CachingLinkedDataSource) {
      ((CachingLinkedDataSource)linkedDataSource).invalidate(fromCon);
    }
    logger.debug("fromCon {}, stateOfSenderBeforeSending{}", fromCon, action.getStateOfSenderBeforeSending());
    Dataset dataModel = linkedDataSource.getDataForResource(fromCon);

    logger.debug("crawled dataset for fromCon {}: {}", fromCon, RdfUtils.toString(dataModel));

    String sparqlPrefix =
      "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>"+
        "PREFIX geo:   <http://www.w3.org/2003/01/geo/wgs84_pos#>"+
        "PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>"+
        "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+
        "PREFIX won:   <http://purl.org/webofneeds/model#>"+
        "PREFIX wontx:   <http://purl.org/webofneeds/tx/model#>"+
        "PREFIX gr:    <http://purl.org/goodrelations/v1#>"+
        "PREFIX sioc:  <http://rdfs.org/sioc/ns#>"+
        "PREFIX ldp:   <http://www.w3.org/ns/ldp#>";

    String queryString = sparqlPrefix +
      "ASK WHERE { ?con wontx:hasBAState ?state }";

    QuerySolutionMap binding = new QuerySolutionMap();
    binding.add("con", new ResourceImpl(fromCon.toString()));
    binding.add("state", new ResourceImpl(action.getStateOfSenderBeforeSending().toString()));
    Query query = QueryFactory.create(queryString);
    try (QueryExecution qExec = QueryExecutionFactory.create(query, dataModel, binding)) {
        boolean result = qExec.execAsk();
        //check if the connection is really in the state required for the action
        if (result) return;
        //we detected an error. Throw an exception.
        //query again, this time fetch the state so we can display an informaitive error message
        queryString = sparqlPrefix +
          "SELECT ?state WHERE { ?con wontx:hasBAState ?state }";
        binding = new QuerySolutionMap();
        binding.add("con", new ResourceImpl(fromCon.toString()));
        query = QueryFactory.create(queryString);
    }
    try (QueryExecution qExec = QueryExecutionFactory.create(query, dataModel, binding)){
        ResultSet res = qExec.execSelect();
        if (! res.hasNext()) {
          throw new IllegalStateException("connection state of connection " + fromCon +" does " +
            "not allow next action " + action +". Could not determine actual connection state: not found");
        }
        QuerySolution solution = res.next();
        RDFNode state = solution.get("state");
        throw new IllegalStateException("connection state " + state + " of connection " + fromCon +" does " +
          "not allow next action " + action);
    }
  }

  private boolean bothConnectionURIsAreKnown() {
    return this.participantSideConnectionURI != null && this.coordinatorSideConnectionURI != null;
  }

  private void sendMessage(final BATestScriptAction action, final URI fromCon, final URI fromNeed, final URI toCon,
                           final URI toNeed, Date when) throws
    Exception {
    assert action != null : "action must not be null";
    assert fromCon != null : "fromCon must not be null";
    assert when != null : "when must not be null";

    logger.debug("scheduling connection message for date {}", when);
    getEventListenerContext().getTaskScheduler().schedule(new Runnable()
    {
      public void run() {
        try {
          getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessageForConnectionMessage(
            fromCon, fromNeed, toCon, toNeed,
            action.getMessageToBeSent()));
        } catch (Exception e) {
          logger.warn("could not send message from {} ", fromCon);
          logger.warn("caught exception", e);
        }
      }
    }, when);
  }

  private WonMessage createWonMessageForConnectionMessage(URI fromConUri, URI fromNeedUri, URI toConUri, URI toNeedUri,
                                                          Model content)
    throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    Dataset localNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(fromNeedUri);
    Dataset remoteNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(toNeedUri);

    URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(localNeedRDF, fromNeedUri);
    URI remoteWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, toNeedUri);

    return  WonMessageBuilder
      .setMessagePropertiesForConnectionMessage(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        fromConUri,
        fromNeedUri,
        localWonNode,
        toConUri,
        toNeedUri,
        remoteWonNode,
        content)
      .build();
  }

  private WonMessage createWonMessageForOpen(URI fromConUri, URI fromNeedUri, URI toConUri, URI toNeedUri)
    throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    Dataset localNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(fromNeedUri);
    Dataset remoteNeedRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(toNeedUri);

    URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(localNeedRDF, fromNeedUri);
    URI remoteWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, toNeedUri);

    return  WonMessageBuilder
      .setMessagePropertiesForOpen(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        fromConUri,
        fromNeedUri,
        localWonNode,
        toConUri,
        toNeedUri,
        remoteWonNode,null)
      .build();
  }

  private void sendOpen(final URI connectionURI, Date when) throws Exception {
    assert connectionURI != null : "connectionURI must not be null";
    assert when != null : "when must not be null";
    logger.debug("scheduling connection message for date {}",when);
    getEventListenerContext().getTaskScheduler().schedule(new Runnable()
    {
      public void run()
      {
        try {
          //TODO: THIS STILL HAS TO BE ADAPTED TO NEW MESSAGE FORMAT!
          //getEventListenerContext().getWonMessageSender().sendWonMessage(connectionURI, null, null);
          throw new UnsupportedOperationException("Not yet adapted to new message format!");
        } catch (Exception e) {
          logger.warn("could not send open from {} ", connectionURI);
          logger.warn("caught exception", e);
        }
      }
    }, when);
  }

  private URI getConnectionToSendFrom(final boolean senderIsCoordinator) {
    return senderIsCoordinator ? coordinatorSideConnectionURI : participantSideConnectionURI;
  }

  private URI getNeedToSendFrom(final boolean senderIsCoordinator) {
    return senderIsCoordinator ? coordinatorURI : participantURI;
  }

  /**
   * Checks whether the event is really about the connection between coordinator and participant by
   * fetching the linked data description for the connection and checking the remoteNeed URI.
   */
  private boolean isRelevantEvent(final Event event, final URI needURI, final URI connectionURI) {
    URI remoteNeedURI = ((RemoteNeedSpecificEvent)event).getRemoteNeedURI();
    if (remoteNeedURI == null){
      logger.debug("remote need URI not found in event data, fetching linked data for {}", connectionURI);
      remoteNeedURI = WonLinkedDataUtils.getRemoteNeedURIforConnectionURI(connectionURI,
        getEventListenerContext().getLinkedDataSource());
    }
    if (this.coordinatorURI.equals(needURI) && this.participantURI.equals(remoteNeedURI)){
      return true;
    } else if (this.participantURI.equals(needURI) && this.coordinatorURI.equals(remoteNeedURI)){
      return true;
    } else {
      return false;
    }
  }

  private void rememberConnectionURI(final URI needURI, final URI connectionURI) {
    if (this.coordinatorURI.equals(needURI)){
      this.coordinatorSideConnectionURI = connectionURI;
      addConnectionURIToFilter(connectionURI);
    } else if (this.participantURI.equals(needURI)){
      this.participantSideConnectionURI = connectionURI;
      addConnectionURIToFilter(connectionURI);
    } else {
      throw new IllegalStateException(new MessageFormat("Listener called for need {0}, " +
        "which is neither my coordinator {1} nor my " +
        "participant {2}").format(new Object[]{needURI, this.coordinatorURI, this.participantURI}));
    }
  }

  private void addConnectionURIToFilter(final URI connectionURI) {
    AndFilter filter = (AndFilter)this.eventFilter;
    for (EventFilter subFilter: filter.getFilters()){
      if (subFilter instanceof OrFilter){
        ((OrFilter)subFilter).addFilter(new ConnectionUriEventFilter(connectionURI));
        break;
      }
    }
  }

  public void updateFilterForBothConnectionURIs() {
    OrFilter filter = new OrFilter();
    filter.addFilter(new ConnectionUriEventFilter(this.coordinatorSideConnectionURI));
    filter.addFilter(new ConnectionUriEventFilter(this.participantSideConnectionURI));
    this.eventFilter = filter;
  }

  private void addIrrelevantConnectionURIToFilter(final URI connectionURI) {
    AndFilter filter = (AndFilter)this.eventFilter;
    filter.addFilter(new NotFilter(new ConnectionUriEventFilter(connectionURI)));
  }

  private boolean isConnectionURIKnown(URI connectionURI) {
    assert connectionURI != null : "connectionURI must not be null";
    return connectionURI.equals(this.coordinatorSideConnectionURI) || connectionURI.equals(this
      .participantSideConnectionURI);
  }

  public URI getCoordinatorURI() {
    return coordinatorURI;
  }

  public URI getParticipantURI() {
    return participantURI;
  }

  public URI getCoordinatorSideConnectionURI() {
    return coordinatorSideConnectionURI;
  }

  public URI getParticipantSideConnectionURI() {
    return participantSideConnectionURI;
  }

  public void setCoordinatorSideConnectionURI(final URI coordinatorSideConnectionURI) {
    this.coordinatorSideConnectionURI = coordinatorSideConnectionURI;
  }

  public void setParticipantSideConnectionURI(final URI participantSideConnectionURI) {
    this.participantSideConnectionURI = participantSideConnectionURI;
  }

  @Override
  public String toString() {
    return "BATestScriptListener" + "{" +
      "name=" + name +
      ", coordinatorURI=" + coordinatorURI +
      ", participantURI=" + participantURI +
      ", coordinatorSideConnectionURI=" + coordinatorSideConnectionURI +
      ", participantSideConnectionURI=" + participantSideConnectionURI +
      ", messagesInFlight=" + messagesInFlight +
      ", millisBetweenMessages=" + millisBetweenMessages +
      '}';
  }
}
