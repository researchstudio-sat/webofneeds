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

package won.bot.framework.events.listener.baStateBots;

import won.bot.framework.events.Event;
import won.bot.framework.events.event.ConnectFromOtherNeedEvent;
import won.bot.framework.events.event.ConnectionSpecificEvent;
import won.bot.framework.events.event.NeedSpecificEvent;
import won.bot.framework.events.event.OpenFromOtherNeedEvent;
import won.bot.framework.events.listener.AbstractFinishingListener;
import won.bot.framework.events.listener.EventFilter;
import won.bot.framework.events.listener.EventListenerContext;
import won.bot.framework.events.listener.filter.NeedUriEventFilter;
import won.bot.framework.events.listener.filter.OrFilter;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;

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
  private Object countMonitor = new Object();
  private long millisBetweenMessages = 10;

  public BATestScriptListener(final EventListenerContext context, final BATestBotScript script,
    final URI coordinatorURI, final URI participantURI, long millisBetweenMessages) {
    super(context, createEventFilter(coordinatorURI, participantURI));
    this.script = script;
    this.coordinatorURI = coordinatorURI;
    this.participantURI = participantURI;
    this.millisBetweenMessages = millisBetweenMessages;
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
    OrFilter filter = new OrFilter();
    filter.addFilter(new NeedUriEventFilter(coordinatorURI));
    filter.addFilter(new NeedUriEventFilter(participantURI));
    return filter;
  }

  @Override
  public boolean isFinished() {
    synchronized (countMonitor) {
      return (!script.hasNext()) && messagesInFlight == 0;
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
    URI needURI = ((NeedSpecificEvent) event).getNeedURI();
    URI connectionURI = ((ConnectionSpecificEvent) event).getConnectionURI();
    rememberConnectionURI(event, needURI, connectionURI);
    if (event instanceof ConnectFromOtherNeedEvent){
      //send an automatic open
      sendOpen(connectionURI, new Date(System.currentTimeMillis() + millisBetweenMessages));
      return;
    }
    if (this.script.hasNext()){
      //if there is an action, execute it.
      BATestScriptAction action = this.script.getNextAction();
      URI fromCon = getConnectionToSendFrom(action.isSenderIsCoordinator());
      sendMessage(action, fromCon, new Date(System.currentTimeMillis() + millisBetweenMessages));
      synchronized (countMonitor){
        this.messagesInFlight++;
      }
    }
    //in any case: remember that we processed a message. Especially important for the message sent
    //through the last action, which we have to process as well otherwise the listener will finish too early
    //which may cause the bot to finish and the whole application to shut down before all messages have been
    //received, which leads to ugly exceptions
    synchronized (countMonitor){
      this.messagesInFlight--;
    }
  }

  private void sendMessage(final BATestScriptAction action, final URI fromCon, Date when) throws Exception {
    logger.debug("scheduling connection message for date {}",when);
    getEventListenerContext().getTaskScheduler().schedule(new Runnable()
    {
      public void run()
      {
        try {
          getEventListenerContext().getOwnerService().textMessage(fromCon, action.getMessageToBeSent());
        } catch (Exception e) {
          logger.warn("could not send message from {} ", fromCon);
          logger.warn("caught exception", e);
        }
      }
    }, when);
  }

  private void sendOpen(final URI connectionURI, Date when) throws Exception {
    logger.debug("scheduling connection message for date {}",when);
    getEventListenerContext().getTaskScheduler().schedule(new Runnable()
    {
      public void run()
      {
        try {
          getEventListenerContext().getOwnerService().open(connectionURI, null);
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

  private void rememberConnectionURI(final Event event, final URI needURI, final URI connectionURI) {
    if (event instanceof ConnectFromOtherNeedEvent){
      //stage1: connection initiated. not yet open. remember our connection URI
      rememberURI(needURI, connectionURI);
    }
    if (event instanceof OpenFromOtherNeedEvent){
      //stage2: connection was accepted. continue with sending messages, but remember our connection URI first
      rememberURI(needURI, connectionURI);
    }
  }

  private void rememberURI(final URI needURI, final URI connectionURI) {
    if (this.coordinatorURI.equals(needURI)){
      this.coordinatorSideConnectionURI = connectionURI;
    } else if (this.participantURI.equals(needURI)){
      this.participantSideConnectionURI = connectionURI;
    } else {
      throw new IllegalStateException(new MessageFormat("Listener called for need {0}, " +
        "which is neither my coordinator {1} nor my " +
        "participant {2}").format(new Object[]{needURI, this.coordinatorURI, this.participantURI}));
    }
  }


}
