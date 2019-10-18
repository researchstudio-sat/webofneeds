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
package won.bot.framework.eventbot.filter.impl;

import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.AbstractDelegatingFilter;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Delegating filter that counts how many times its delegate has accepted. Will
 * only accept a given number of times.
 */
public class AcceptNTimesFilter extends AbstractDelegatingFilter {
    private int count = 0;
    private int targetCount;

    public AcceptNTimesFilter(final EventFilter delegate, final int targetCount) {
        super(delegate);
        this.targetCount = targetCount;
    }

    @Override
    public boolean accept(final Event event) {
        boolean delegateAccepts = getDelegate().accept(event);
        if (!delegateAccepts)
            return false;
        synchronized (this) {
            if (count < targetCount) {
                count++;
                return true;
            }
        }
        return false;
    }
}
