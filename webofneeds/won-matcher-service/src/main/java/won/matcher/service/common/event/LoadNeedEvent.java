package won.matcher.service.common.event;

import java.io.Serializable;

/**
 * Created by hfriedrich on 17.10.2016.
 * <p>
 * Event is used to aks the crawler to load events that were either the last X
 * needs seen or saved during a given date interval.
 */
public class LoadNeedEvent implements Serializable {
  private long fromDate;
  private long toDate;
  private int lastXNeedEvents;

  /**
   * Request all need events between fromDate and toDate (matcher service
   * timestamp)
   *
   * @param fromDate
   * @param toDate
   */
  public LoadNeedEvent(long fromDate, long toDate) {
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.lastXNeedEvents = -1;
  }

  /**
   * Request last X need events from matcher service
   *
   * @param lastXNeedEvents
   */
  public LoadNeedEvent(int lastXNeedEvents) {
    this.fromDate = -1;
    this.toDate = -1;
    this.lastXNeedEvents = lastXNeedEvents;
  }

  public long getFromDate() {
    return fromDate;
  }

  public long getToDate() {
    return toDate;
  }

  public int getLastXNeedEvents() {
    return lastXNeedEvents;
  }

  @Override
  public String toString() {

    if (lastXNeedEvents != -1) {
      return "[last " + lastXNeedEvents + " need events]";
    }
    return "date interval: [" + fromDate + "," + toDate + "]";
  }
}
