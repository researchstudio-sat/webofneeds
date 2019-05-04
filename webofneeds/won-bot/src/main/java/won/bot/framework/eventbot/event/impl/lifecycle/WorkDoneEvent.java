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
package won.bot.framework.eventbot.event.impl.lifecycle;

import won.bot.framework.bot.Bot;
import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Event indicating that the bot's work is done. This event is informative only
 * and has no effect on the bot framework. It is used for testing, though:
 * integration tests subscribe to this event and run their asserts after seeing
 * it.
 */
public class WorkDoneEvent extends BaseEvent {
    private Bot bot;

    public Bot getBot() {
        return bot;
    }

    public WorkDoneEvent(Bot bot) {
        this.bot = bot;
    }
}
