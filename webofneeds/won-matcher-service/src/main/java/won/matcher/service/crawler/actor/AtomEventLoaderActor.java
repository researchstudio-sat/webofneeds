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
import won.matcher.service.common.event.BulkAtomEvent;
import won.matcher.service.common.event.LoadAtomEvent;
import won.matcher.service.crawler.service.CrawlSparqlService;

/**
 * Created by hfriedrich on 17.10.2016. Actor that loads crawled and saved atom
 * events from the rdf store the and sends them back to the actor requesting it
 */
@Component
@Scope("prototype")
public class AtomEventLoaderActor extends UntypedActor {
    private static int MAX_BULK_SIZE = 10;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef pubSubMediator;
    @Autowired
    private CrawlSparqlService sparqlService;

    @Override
    public void preStart() {
        // subscribe for load atom events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(LoadAtomEvent.class.getName(), getSelf()),
                        getSelf());
    }

    @Override
    public void onReceive(final Object o) throws Throwable {
        if (o instanceof LoadAtomEvent) {
            LoadAtomEvent msg = (LoadAtomEvent) o;
            log.debug("received request to load atoms events: {}", msg);
            BulkAtomEvent bulkAtomEvent;
            int offset = 0;
            do {
                // check if atom event should be returned in time interval or last X atom events
                if (msg.getLastXAtomEvents() == -1) {
                    bulkAtomEvent = sparqlService.retrieveActiveAtomEvents(msg.getFromDate(), msg.getToDate(), offset,
                                    MAX_BULK_SIZE, true);
                } else {
                    bulkAtomEvent = sparqlService.retrieveActiveAtomEvents(0, Long.MAX_VALUE, offset,
                                    Math.min(MAX_BULK_SIZE, msg.getLastXAtomEvents() - offset), false);
                }
                if (bulkAtomEvent.getAtomEvents().size() > 0) {
                    log.debug("send bulk event of size {} back to requesting actor",
                                    bulkAtomEvent.getAtomEvents().size());
                    getSender().tell(bulkAtomEvent, getSelf());
                    offset += bulkAtomEvent.getAtomEvents().size();
                }
            } while (bulkAtomEvent.getAtomEvents().size() == MAX_BULK_SIZE);
        }
    }
}
