package won.matcher.service.common.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Event can hold multiple {@link AtomEvent} objects
 */
public class BulkAtomEvent implements Serializable {
    private Collection<AtomEvent> atomEvents;

    public BulkAtomEvent() {
        atomEvents = new LinkedList<>();
    }

    public void addAtomEvent(AtomEvent atomEvent) {
        atomEvents.add(atomEvent);
    }

    public Collection<AtomEvent> getAtomEvents() {
        return Collections.unmodifiableCollection(atomEvents);
    }
}