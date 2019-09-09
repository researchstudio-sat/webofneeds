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

import java.net.URI;

import won.bot.framework.eventbot.event.AtomSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Abstract base class for filters that will only accept events (atom specific
 * or connection specific) with the specified atom URI.
 */
public class AtomUriEventFilter implements EventFilter {
    private URI atomURI;

    public AtomUriEventFilter(final URI atomURI) {
        this.atomURI = atomURI;
    }

    /**
     * Factory method for creating a filter from an event by using its atom URI.
     * 
     * @param event
     * @return the filter or null if no atom URI could be obtained from the event.
     */
    public static AtomUriEventFilter forEvent(Event event) {
        URI atomUri = getAtomUriFromEvent(event);
        if (atomUri == null)
            return null;
        return new AtomUriEventFilter(atomUri);
    }

    @Override
    public boolean accept(final Event event) {
        URI atomUriOfEvent = getAtomUriFromEvent(event);
        if (atomUriOfEvent == null)
            return false;
        return atomUriOfEvent.equals(this.atomURI);
    }

    public URI getAtomURI() {
        return atomURI;
    }

    private static URI getAtomUriFromEvent(final Event event) {
        if (event instanceof AtomSpecificEvent) {
            return ((AtomSpecificEvent) event).getAtomURI();
        }
        return null;
    }
}
