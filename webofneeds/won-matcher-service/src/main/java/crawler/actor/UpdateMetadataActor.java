package crawler.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.config.CommonSettings;
import common.config.CommonSettingsImpl;
import crawler.config.CrawlSettings;
import crawler.config.CrawlSettingsImpl;
import crawler.msg.CrawlUriMessage;
import crawler.service.CrawlSparqlService;

import java.util.Collection;
import java.util.LinkedList;

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
  private final CommonSettingsImpl commonSettings = CommonSettings.SettingsProvider.get(getContext().system());
  private final CrawlSettingsImpl settings = CrawlSettings.SettingsProvider.get(getContext().system());
  private CrawlSparqlService endpoint;
  private Collection<CrawlUriMessage> bulkMessages;
  private static final String TICK = "tick";

  public UpdateMetadataActor() {
    endpoint = new CrawlSparqlService(commonSettings.SPARQL_ENDPOINT);
    bulkMessages = new LinkedList<>();
  }

  @Override
  public void preStart() {

    // Execute the bulk update at least once a while even if not enough messages are there
    getContext().system().scheduler().schedule(settings.METADATA_UPDATE_DURATION,
      settings.METADATA_UPDATE_DURATION, getSelf(), TICK, getContext().dispatcher(), null);
  }

  @Override
  public void postStop() {

    // execute update for the remaining messages before stop
    update();
  }

  /**
   * Collects messages until the maximum bulk update size is reached or a timer is
   * elapsed to execute the meta data bulk update.
   *
   * @param message
   */
  @Override
  public void onReceive(final Object message) {
    if (message instanceof CrawlUriMessage) {
      CrawlUriMessage uriMsg = (CrawlUriMessage) message;
      log.debug("Add message to bulk update list: {}", uriMsg);
      bulkMessages.add(uriMsg);
      if (bulkMessages.size() >= settings.METADATA_UPDATE_MAX_BULK_SIZE) {
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
