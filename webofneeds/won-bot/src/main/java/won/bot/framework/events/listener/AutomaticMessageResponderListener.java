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

package won.bot.framework.events.listener;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.events.event.MessageFromOtherNeedEvent;
import won.bot.framework.events.event.OpenFromOtherNeedEvent;
import won.bot.framework.events.Event;
import won.protocol.model.ConnectionState;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Date;

/**
 * Listener that responds to open and message events with automatic messages.
 * Can be configured to apply a timeout (non-blocking) before sending messages.
 * Can be configured to send a fixed number of messages and then unsubscribe from events.
 */
public class AutomaticMessageResponderListener extends AbstractHandleFirstNEventsListener
{
  private long millisTimeoutBeforeReply = 1000;

  public AutomaticMessageResponderListener(final EventListenerContext context, final int targetNumberOfMessages, final long millisTimeoutBeforeReply)
  {
    super(context, targetNumberOfMessages);
    this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
  }

  public AutomaticMessageResponderListener(final EventListenerContext context, final EventFilter eventFilter, final int targetCount, final long millisTimeoutBeforeReply)
  {
    super(context, eventFilter, targetCount);
    this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
  }


  @Override
  protected void handleFirstNTimes(final Event event) throws Exception
  {
    if (event instanceof MessageFromOtherNeedEvent){
      handleMessageEvent((MessageFromOtherNeedEvent) event);
    } else if (event instanceof OpenFromOtherNeedEvent) {
      handleOpenEvent((OpenFromOtherNeedEvent) event);
    }
  }

  /**
   * React to open event by sending a message.
   *
   * @param openEvent
   */
  private void handleOpenEvent(final OpenFromOtherNeedEvent openEvent)
  {
    logger.debug("got open event for need: {}, connection state is: {}", openEvent.getCon().getNeedURI(), openEvent.getCon().getState());
    if (openEvent.getCon().getState() == ConnectionState.CONNECTED){
      logger.debug("replying to open with message (delay: {} millis)", millisTimeoutBeforeReply);
      getEventListenerContext().getTaskScheduler().schedule(new Runnable()
      {
        @Override
        public void run()
        {
          URI connectionUri = openEvent.getCon().getConnectionURI();
          try {
            getEventListenerContext().getOwnerService().textMessage(connectionUri, WonRdfUtils.MessageUtils.textMessage(createMessage()));
          } catch (Exception e) {
            logger.warn("could not send message via connection {}", connectionUri, e);
          }
        }
      }, new Date(System.currentTimeMillis() + millisTimeoutBeforeReply));
    }
  }

  private void handleMessageEvent(final MessageFromOtherNeedEvent messageEvent){
    logger.debug("got message '{}' for need: {}", messageEvent.getMessage().getMessage(), messageEvent.getCon().getNeedURI());
    getEventListenerContext().getTaskScheduler().schedule(new Runnable()
    {
      @Override
      public void run()
      {
        String message = createMessage();
        Model messageContent = WonRdfUtils.MessageUtils.textMessage(message);
        URI connectionUri = messageEvent.getCon().getConnectionURI();
        logger.debug("sending message " + message);
        try {
          getEventListenerContext().getOwnerService().textMessage(connectionUri, messageContent);
        } catch (Exception e) {
          logger.warn("could not send message via connection {}", connectionUri, e);
        }
      }
    }, new Date(System.currentTimeMillis() + this.millisTimeoutBeforeReply));
  }


  private String createMessage()
  {
    String message = "auto reply no " + (getCount());
    if (getTargetCount() > 0){
      message += " of " + getTargetCount();
    }
    message +=  "(delay: "+ millisTimeoutBeforeReply + " millis)";
    return message;
  }

  @Override
  protected void unsubscribe()
  {
    logger.debug("unsubscribing from all events");
    getEventListenerContext().getEventBus().unsubscribe(this);
  }
}
