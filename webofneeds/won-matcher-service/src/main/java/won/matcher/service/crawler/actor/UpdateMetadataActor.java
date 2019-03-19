package won.matcher.service.crawler.actor;

import java.util.Collection;
import java.util.LinkedList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import won.matcher.service.crawler.config.CrawlConfig;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.matcher.service.crawler.service.CrawlSparqlService;

/**
 * Actor that updates the meta data of the crawling of URIs (baseUri, date, status) in a Sparql endpoint. This is used
 * to know which URIs have already been crawled and for which URIs the crawling is still running or failed. Also the
 * actor collects a certain number of messages before it updates the meta data in a single query bulk update for all of
 * them.
 *
 * User: hfriedrich Date: 17.04.2015
 */
@Component
@Scope("prototype")
public class UpdateMetadataActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Collection<CrawlUriMessage> bulkMessages = new LinkedList<>();
    private static final String TICK = "tick";

    @Autowired
    private CrawlConfig config;

    @Autowired
    private CrawlSparqlService endpoint;

    @Override
    public void preStart() {

        // Execute the bulk update at least once a while even if not enough messages are there
        getContext().system().scheduler().schedule(config.getMetaDataUpdateMaxDuration(),
                config.getMetaDataUpdateMaxDuration(), getSelf(), TICK, getContext().dispatcher(), null);
    }

    @Override
    public void postStop() {

        // execute update for the remaining messages before stop
        update();
    }

    /**
     * Collects messages until the maximum bulk update size is reached or a timer is elapsed to execute the meta data
     * bulk update.
     *
     * @param message
     */
    @Override
    public void onReceive(final Object message) {
        if (message instanceof CrawlUriMessage) {
            CrawlUriMessage uriMsg = (CrawlUriMessage) message;
            log.debug("Add message to bulk update list: {}", uriMsg);
            bulkMessages.add(uriMsg);
            if (bulkMessages.size() >= config.getMetaDataUpdateMaxBulkSize()) {
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

    @Override
    public SupervisorStrategy supervisorStrategy() {

        SupervisorStrategy supervisorStrategy = new OneForOneStrategy(0, Duration.Zero(),
                new Function<Throwable, SupervisorStrategy.Directive>() {

                    @Override
                    public SupervisorStrategy.Directive apply(Throwable t) throws Exception {

                        log.warning("Actor encountered error: {}", t);
                        // default behaviour
                        return SupervisorStrategy.escalate();
                    }
                });

        return supervisorStrategy;
    }
}
