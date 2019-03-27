package won.matcher.service.crawler.actor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.LoadNeedEvent;
import won.matcher.service.crawler.service.CrawlSparqlService;

/**
 * Created by hfriedrich on 17.10.2016. Actor that loads crawled and saved need
 * events from the rdf store the and sends them back to the actor requesting it
 */
@Component
@Scope("prototype")
public class NeedEventLoaderActor extends UntypedActor {
    private static int MAX_BULK_SIZE = 10;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef pubSubMediator;
    @Autowired
    private CrawlSparqlService sparqlService;

    @Override
    public void preStart() {
        // subscribe for load need events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(LoadNeedEvent.class.getName(), getSelf()),
                        getSelf());
    }

    @Override
    public void onReceive(final Object o) throws Throwable {
        if (o instanceof LoadNeedEvent) {
            LoadNeedEvent msg = (LoadNeedEvent) o;
            log.debug("received request to load needs events: {}", msg);
            BulkNeedEvent bulkNeedEvent;
            int offset = 0;
            do {
                // check if need event should be returned in time interval or last X need events
                if (msg.getLastXNeedEvents() == -1) {
                    bulkNeedEvent = sparqlService.retrieveActiveNeedEvents(msg.getFromDate(), msg.getToDate(), offset,
                                    MAX_BULK_SIZE, true);
                } else {
                    bulkNeedEvent = sparqlService.retrieveActiveNeedEvents(0, Long.MAX_VALUE, offset,
                                    Math.min(MAX_BULK_SIZE, msg.getLastXNeedEvents() - offset), false);
                }
                if (bulkNeedEvent.getNeedEvents().size() > 0) {
                    log.debug("send bulk event of size {} back to requesting actor",
                                    bulkNeedEvent.getNeedEvents().size());
                    getSender().tell(bulkNeedEvent, getSelf());
                    offset += bulkNeedEvent.getNeedEvents().size();
                }
            } while (bulkNeedEvent.getNeedEvents().size() == MAX_BULK_SIZE);
        }
    }
}
