package won.matcher.service.nodemanager.actor;

import java.net.URI;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedConsumerActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.event.Cause;
import won.matcher.service.common.service.monitoring.MonitoringService;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.matcher.service.crawler.msg.ResourceCrawlUriMessage;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * Camel actor represents the atom consumer protocol to a won node. It is used
 * to listen at the WoN node for events about new created atoms, activated atoms
 * and deactivated atoms. Depending on the endpoint it might be used with
 * different protocols (e.g. JMS over activemq). User: hfriedrich Date:
 * 28.04.2015
 */
@Component
@Scope("prototype")
public class AtomConsumerProtocolActor extends UntypedConsumerActor {
    private static final String MSG_HEADER_METHODNAME = "methodName";
    private static final String MSG_HEADER_METHODNAME_ATOMCREATED = "atomCreated";
    private static final String MSG_HEADER_METHODNAME_ATOMMODIFIED = "atomModified";
    private static final String MSG_HEADER_METHODNAME_ATOMACTIVATED = "atomActivated";
    private static final String MSG_HEADER_METHODNAME_ATOMDEACTIVATED = "atomDeactivated";
    private static final String MSG_HEADER_WON_NODE_URI = "wonNodeURI";
    private static final String MSG_HEADER_ATOM_URI = "atomURI";
    private final String endpoint;
    private ActorRef pubSubMediator;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    @Autowired
    private MonitoringService monitoringService;
    @Autowired
    private LinkedDataSource linkedDataSource;

    public AtomConsumerProtocolActor(String endpoint) {
        this.endpoint = endpoint;
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    }

    @Override
    public String getEndpointUri() {
        return endpoint;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof CamelMessage) {
            CamelMessage camelMsg = (CamelMessage) message;
            String atomUri = (String) camelMsg.getHeaders().get(MSG_HEADER_ATOM_URI);
            String wonNodeUri = (String) camelMsg.getHeaders().get(MSG_HEADER_WON_NODE_URI);
            // monitoring code
            monitoringService.startClock(MonitoringService.ATOM_HINT_STOPWATCH, atomUri);
            // process the incoming atom event
            if (atomUri != null && wonNodeUri != null) {
                Object methodName = camelMsg.getHeaders().get(MSG_HEADER_METHODNAME);
                if (methodName != null) {
                    log.debug("Received event '{}' for atomUri '{}' and wonAtomUri '{}' and publish it to matchers",
                                    methodName, atomUri, wonNodeUri);
                    // publish an atom event to all the (distributed) matchers
                    AtomEvent event = null;
                    long crawlDate = System.currentTimeMillis();
                    Dataset ds = linkedDataSource.getDataForResource(URI.create(atomUri));
                    if (AtomModelWrapper.isAAtom(ds)) {
                        if (methodName.equals(MSG_HEADER_METHODNAME_ATOMCREATED)) {
                            event = new AtomEvent(atomUri, wonNodeUri, AtomEvent.TYPE.ACTIVE, crawlDate, ds,
                                            Cause.PUSHED);
                            pubSubMediator.tell(
                                            new DistributedPubSubMediator.Publish(event.getClass().getName(), event),
                                            getSelf());
                        } else if (methodName.equals(MSG_HEADER_METHODNAME_ATOMMODIFIED)) {
                            event = new AtomEvent(atomUri, wonNodeUri, AtomEvent.TYPE.ACTIVE, crawlDate, ds,
                                            Cause.PUSHED);
                            pubSubMediator.tell(
                                            new DistributedPubSubMediator.Publish(event.getClass().getName(), event),
                                            getSelf());
                        } else if (methodName.equals(MSG_HEADER_METHODNAME_ATOMACTIVATED)) {
                            event = new AtomEvent(atomUri, wonNodeUri, AtomEvent.TYPE.ACTIVE, crawlDate, ds,
                                            Cause.PUSHED);
                            pubSubMediator.tell(
                                            new DistributedPubSubMediator.Publish(event.getClass().getName(), event),
                                            getSelf());
                        } else if (methodName.equals(MSG_HEADER_METHODNAME_ATOMDEACTIVATED)) {
                            event = new AtomEvent(atomUri, wonNodeUri, AtomEvent.TYPE.INACTIVE, crawlDate, ds,
                                            Cause.PUSHED);
                            pubSubMediator.tell(
                                            new DistributedPubSubMediator.Publish(event.getClass().getName(), event),
                                            getSelf());
                        } else {
                            unhandled(message);
                        }
                        // let the crawler save the data of this event too
                        ResourceCrawlUriMessage resMsg = new ResourceCrawlUriMessage(atomUri, atomUri, wonNodeUri,
                                        CrawlUriMessage.STATUS.SAVE, crawlDate, null);
                        resMsg.setSerializedResource(camelMsg.body().toString());
                        resMsg.setSerializationFormat(Lang.TRIG);
                        pubSubMediator.tell(new DistributedPubSubMediator.Publish(resMsg.getClass().getName(), resMsg),
                                        getSelf());
                        return;
                    }
                } else {
                    log.warning("Message not processed; methodName is null");
                }
            } else {
                log.warning("Message not processed; atomURI or wonNodeURI is null");
            }
        }
        unhandled(message);
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
