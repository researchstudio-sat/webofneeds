/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.framework.eventbot.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.filter.impl.LocalResponseEventFilter;
import won.bot.framework.eventbot.filter.impl.RemoteResponseEventFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * User: fkleedorfer Date: 02.02.14
 */
public class EventBotActionUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Creates an anonymous instance of <code>BaseEventBotAction</code> that
     * executes the specified action.
     *
     * @param ctx
     * @param action
     * @return
     */
    public static BaseEventBotAction makeAction(EventListenerContext ctx, Consumer<Event> action) {
        return new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener)
                            throws Exception {
                action.accept(event);
            }
        };
    }

    /**
     * Creates an anonymous instance of <code>BaseEventBotAction</code> that
     * executes the specified action.
     *
     * @param ctx
     * @param action
     * @return
     */
    public static BaseEventBotAction makeActionWithListenerRef(EventListenerContext ctx,
                    BiConsumer<Event, EventListenerContext> action) {
        return new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener)
                            throws Exception {
                action.accept(event, getEventListenerContext());
            }
        };
    }

    /**
     * Creates an anonymous instance of <code>BaseEventBotAction</code> that
     * executes the specified action.
     *
     * @param ctx
     * @param action
     * @return
     */
    public static BaseEventBotAction makeActionWithContextRef(EventListenerContext ctx,
                    BiConsumer<Event, EventListener> action) {
        return new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener)
                            throws Exception {
                action.accept(event, executingListener);
            }
        };
    }

    /**
     * @deprecated use
     * {@link won.bot.framework.bot.context.BotContextWrapper#rememberAtomUri(URI)}
     * instead
     */
    @Deprecated
    public static void rememberInList(EventListenerContext ctx, URI uri, String uriListName) {
        if (uriListName != null && uriListName.trim().length() > 0) {
            ctx.getBotContext().appendToUriList(uri, uriListName);
            logger.debug("remembering atom in NamedAtomList {} ", uri);
        } else {
            throw new IllegalArgumentException("'uriListName' must not not be null or empty");
        }
    }

    /**
     * @deprecated use
     * {@link won.bot.framework.bot.context.BotContextWrapper#rememberNodeUri(URI)}
     * instead
     */
    @Deprecated
    public static void rememberInNodeListIfNamePresent(EventListenerContext ctx, URI uri) {
        ctx.getBotContextWrapper().rememberNodeUri(uri);
    }

    /**
     * @deprecated use
     * {@link won.bot.framework.bot.context.BotContextWrapper#removeAtomUri(URI)}
     * instead
     */
    @Deprecated
    public static void removeFromList(EventListenerContext ctx, URI uri, String uriListName) {
        if (uriListName != null && uriListName.trim().length() > 0) {
            ctx.getBotContext().removeAtomUriFromNamedAtomUriList(uri, uriListName);
            logger.debug("removing atom from NamedAtomList {} ", uri);
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
                    final EventListener successCallback, final EventListener failureCallback,
                    EventListenerContext context) {
        // create an event listener that processes the response to the wonMessage we're
        // about to send
        checkMessageURI(outgoingMessage);
        EventListener listener = new ActionOnFirstEventListener(context,
                        LocalResponseEventFilter.forWonMessage(outgoingMessage),
                        new BaseEventBotAction(context) {
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

    public static void checkMessageURI(final WonMessage message) {
        if (!WonMessageUtils.isValidMessageUri(message.getMessageURIRequired())) {
            throw new IllegalArgumentException("Specified message has invalid message URI "
                            + message.getMessageURIRequired()
                            + ", cannot register a response listener. Make sure to prepare the message befor passing it here.");
        }
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
                    final EventListener successCallback, final EventListener failureCallback,
                    EventListenerContext context) {
        // create an event listener that processes the remote response to the wonMessage
        // we're about to send
        checkMessageURI(outgoingMessage);
        EventListener listener = new ActionOnFirstEventListener(context,
                        RemoteResponseEventFilter.forWonMessage(outgoingMessage),
                        new BaseEventBotAction(context) {
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
