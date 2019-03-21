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

package won.bot.framework.eventbot.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.filter.impl.OriginalMessageUriRemoteResponseEventFilter;
import won.bot.framework.eventbot.filter.impl.OriginalMessageUriResponseEventFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.protocol.message.WonMessage;

import java.net.URI;

/**
 * User: fkleedorfer Date: 02.02.14
 */
public class EventBotActionUtils {
  private static final Logger logger = LoggerFactory.getLogger(EventBotActionUtils.class);

  public static void rememberInList(EventListenerContext ctx, URI uri, String uriListName) {
    if (uriListName != null && uriListName.trim().length() > 0) {
      ctx.getBotContext().appendToNamedNeedUriList(uri, uriListName);
      logger.debug("remembering need in NamedNeedList {} ", uri);
    } else {
      throw new IllegalArgumentException("'uriListName' must not not be null or empty");
    }
  }

  public static void rememberInNodeListIfNamePresent(EventListenerContext ctx, URI uri) {
    ctx.getBotContext().rememberNodeUri(uri);
  }

  public static void removeFromList(EventListenerContext ctx, URI uri, String uriListName) {
    if (uriListName != null && uriListName.trim().length() > 0) {
      ctx.getBotContext().removeNeedUriFromNamedNeedUriList(uri, uriListName);
      logger.debug("removing need from NamedNeedList {} ", uri);
    } else {
      throw new IllegalArgumentException("'uriListName' must not not be null or empty");
    }
  }

  // ************************************************ EventListener
  // ***************************************************

  /**
   * Creates a listener that waits for the response to the specified message. If a
   * SuccessResponse is received, the successCallback is executed, if a
   * FailureResponse is received, the failureCallback is executed.
   *
   * @param outgoingMessage
   * @param successCallback
   * @param failureCallback
   * @param context
   * @return
   */
  public static EventListener makeAndSubscribeResponseListener(final WonMessage outgoingMessage,
      final EventListener successCallback, final EventListener failureCallback, EventListenerContext context) {

    // create an event listener that processes the response to the wonMessage we're
    // about to send
    EventListener listener = new ActionOnFirstEventListener(context,
        OriginalMessageUriResponseEventFilter.forWonMessage(outgoingMessage), new BaseEventBotAction(context) {
          @Override
          protected void doRun(final Event event, EventListener executingListener) throws Exception {
            if (event instanceof SuccessResponseEvent) {
              successCallback.onEvent(event);
            } else if (event instanceof FailureResponseEvent) {
              failureCallback.onEvent(event);
            }
          }
        });
    context.getEventBus().subscribe(SuccessResponseEvent.class, listener);
    context.getEventBus().subscribe(FailureResponseEvent.class, listener);
    return listener;
  }

  /**
   * Creates a listener that waits for the remote response to the specified
   * message. If a SuccessResponse is received, the successCallback is executed,
   * if a FailureResponse is received, the failureCallback is executed.
   *
   * @param outgoingMessage
   * @param successCallback
   * @param failureCallback
   * @param context
   * @return
   */
  public static EventListener makeAndSubscribeRemoteResponseListener(final WonMessage outgoingMessage,
      final EventListener successCallback, final EventListener failureCallback, EventListenerContext context) {

    // create an event listener that processes the remote response to the wonMessage
    // we're about to send
    EventListener listener = new ActionOnFirstEventListener(context,
        OriginalMessageUriRemoteResponseEventFilter.forWonMessage(outgoingMessage), new BaseEventBotAction(context) {
          @Override
          protected void doRun(final Event event, EventListener executingListener) throws Exception {
            if (event instanceof SuccessResponseEvent) {
              successCallback.onEvent(event);
            } else if (event instanceof FailureResponseEvent) {
              failureCallback.onEvent(event);
            }
          }
        });
    context.getEventBus().subscribe(SuccessResponseEvent.class, listener);
    context.getEventBus().subscribe(FailureResponseEvent.class, listener);
    return listener;
  }
}
