package won.matcher.service.common.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Event can hold multiple {@link NeedEvent} objects
 */
public class BulkNeedEvent implements Serializable {
    private Collection<NeedEvent> needEvents;

    public BulkNeedEvent() {
        needEvents = new LinkedList<>();
    }

    public void addNeedEvent(NeedEvent needEvent) {
        needEvents.add(needEvent);
    }

    public Collection<NeedEvent> getNeedEvents() {
        return Collections.unmodifiableCollection(needEvents);
    }
}