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

package won.bot.framework.core.eventlistener;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.core.event.MessageFromOtherNeedEvent;
import won.bot.framework.core.event.OpenFromOtherNeedEvent;
import won.bot.framework.events.Event;
import won.protocol.model.ConnectionState;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Date;

/**
 *
 */
public class AutomaticMessageResponderListener extends BaseEventListener
{
  private int targetNumberOfMessages = -1;
  private int numberOfMessages = 0;
  private long millisTimeoutBeforeReply = 1000;
  private Object monitor = new Object();

  public AutomaticMessageResponderListener(final EventListenerContext context, final int targetNumberOfMessages, final long millisTimeoutBeforeReply)
  {
    super(context);
    this.targetNumberOfMessages = targetNumberOfMessages;
  }

  @Override
  public void onEvent(final Event event) throws Exception
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
    if (openEvent.getCon().getState() == ConnectionState.CONNECTED){
      getEventListenerContext().getTaskScheduler().schedule(new Runnable()
      {
        @Override
        public void run()
        {
          URI connectionUri = openEvent.getCon().getConnectionURI();
          try {
            getEventListenerContext().getOwnerService().textMessage(connectionUri, WonRdfUtils.MessageUtils.textMessage(createMessage()));
          } catch (Exception e){
            logger.warn("could not send message via connection {}", connectionUri,e);
          }
        }
      }, new Date(System.currentTimeMillis() + millisTimeoutBeforeReply));
    }
  }

  private void handleMessageEvent(final MessageFromOtherNeedEvent messageEvent){
    logger.debug("got message '{}' for need: {}", messageEvent.getMessage().getMessage(), messageEvent.getCon().getNeedURI());
    getEventListenerContext().getTaskScheduler().schedule(new Runnable(){
      @Override
      public void run()
      {
        String message = createMessage();
        Model messageContent = WonRdfUtils.MessageUtils.textMessage(message);
        URI connectionUri = messageEvent.getCon().getConnectionURI();
        try {
          getEventListenerContext().getOwnerService().textMessage(connectionUri, messageContent);
          countMessageAndUnsubscribeIfNecessary();
        } catch (Exception e) {
          logger.warn("could not send message via connection {}", connectionUri, e);
        }
      }
    }, new Date(System.currentTimeMillis() + this.targetNumberOfMessages));
  }

  private void countMessageAndUnsubscribeIfNecessary()
  {
    synchronized (monitor){
      numberOfMessages++;
      if (targetNumberOfMessages > 0 && targetNumberOfMessages >= numberOfMessages ){
        unsubscribe();
      }
    }
  }

  private String createMessage()
  {
    String message = "auto reply no " + numberOfMessages;
    if (targetNumberOfMessages > 0){
      message += " of " + targetNumberOfMessages;
    }
    return message;
  }

  private void unsubscribe()
  {
    getEventListenerContext().getEventBus().unsubscribe(MessageFromOtherNeedEvent.class, this);
  }
}
