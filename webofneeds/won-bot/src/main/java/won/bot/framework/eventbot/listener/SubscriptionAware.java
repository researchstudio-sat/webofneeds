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

package won.bot.framework.eventbot.listener;

import won.bot.framework.eventbot.event.Event;

/**
 * Interface to be implemented by listeners that need to know which event types they are subscribed to. The onSubscribe
 * method will be called before the subscription, the onUnscubscribe methods will be called after the listener is
 * unsubscribed.
 */
public interface SubscriptionAware {
    public <T extends Event> void onSubscribe(Class<T> event);

    public <T extends Event> void onUnsubscribe(Class<T> event);
}
