package crawler.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import crawler.db.SparqlEndpointService;
import crawler.message.UriStatusMessage;
import scala.concurrent.duration.Duration;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Actor that updates the meta data of the crawling of URIs (baseUri, date, status) in a Sparql endpoint.
 * This is used to know which URIs have already been crawled and for which URIs the
 * crawling is still running or failed.
 * Also the actor collects a certain number of messages before it updates the meta data in
 * a single query bulk update for all of them.
 *
 * User: hfriedrich
 * Date: 17.04.2015
 */
public class UpdateMetadataActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private SparqlEndpointService endpoint;
  private Collection<UriStatusMessage> bulkMessages;
  private static final int MAX_BULK_SIZE = 10;
  private static final int SCHEDULE_UPDATE_EXECUTION = 10000;
  private static final String TICK = "tick";

  public UpdateMetadataActor(String sparqlEndpoint) {
    endpoint = new SparqlEndpointService(sparqlEndpoint);
    bulkMessages = new LinkedList<>();
  }

  @Override
  public void preStart() {

    // Execute the bulk update at least once a while even if not enough messages are there
    getContext().system().scheduler().schedule(
      Duration.create(SCHEDULE_UPDATE_EXECUTION, TimeUnit.MILLISECONDS),
      Duration.create(SCHEDULE_UPDATE_EXECUTION, TimeUnit.MILLISECONDS),
      getSelf(), TICK, getContext().dispatcher(), null);
  }

  /**
   * Collects messages until the maximum bulk update size is reached or a timer is
   * elapsed to execute the meta data bulk update.
   *
   * @param message
   */
  @Override
  public void onReceive(final Object message) {
    if (message instanceof UriStatusMessage) {
      UriStatusMessage uriMsg = (UriStatusMessage) message;
      log.debug("Add message to bulk update list: {}", uriMsg);
      bulkMessages.add(uriMsg);
      if (bulkMessages.size() >= MAX_BULK_SIZE) {
        update();
      }
    } else if (message instanceof String) {
      update();
    } else {
      unhandled(message);
    }
  }

  /**
   * update meta data for messages available
   */
  private void update() {

    if (bulkMessages.size() > 0) {
      log.debug("Update crawling meta data of {} messages", bulkMessages.size());
      endpoint.bulkUpdateCrawlingMetadata(bulkMessages);
      bulkMessages.clear();
    }
  }
}
