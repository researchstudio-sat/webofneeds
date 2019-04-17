package won.matcher.service.common.event;

import java.io.Serializable;

/**
 * Created by hfriedrich on 17.10.2016. Event is used to aks the crawler to load
 * events that were either the last X atoms seen or saved during a given date
 * interval.
 */
public class LoadAtomEvent implements Serializable {
    private long fromDate;
    private long toDate;
    private int lastXAtomEvents;

    /**
     * Request all atom events between fromDate and toDate (matcher service
     * timestamp)
     *
     * @param fromDate
     * @param toDate
     */
    public LoadAtomEvent(long fromDate, long toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.lastXAtomEvents = -1;
    }

    /**
     * Request last X atom events from matcher service
     *
     * @param lastXAtomEvents
     */
    public LoadAtomEvent(int lastXAtomEvents) {
        this.fromDate = -1;
        this.toDate = -1;
        this.lastXAtomEvents = lastXAtomEvents;
    }

    public long getFromDate() {
        return fromDate;
    }

    public long getToDate() {
        return toDate;
    }

    public int getLastXAtomEvents() {
        return lastXAtomEvents;
    }

    @Override
    public String toString() {
        if (lastXAtomEvents != -1) {
            return "[last " + lastXAtomEvents + " atom events]";
        }
        return "date interval: [" + fromDate + "," + toDate + "]";
    }
}
