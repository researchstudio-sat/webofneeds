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
package won.bot.framework.eventbot.action.impl.counter;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Event indicating that a Counter reached its target count.
 */
public class TargetCountReachedEvent extends BaseEvent {
    private TargetCounterDecorator counter;

    public TargetCountReachedEvent(final TargetCounterDecorator counter) {
        this.counter = counter;
    }

    public Counter getCounter() {
        return counter;
    }

    public int getCount() {
        return this.getCounter().getCount();
    }
}
