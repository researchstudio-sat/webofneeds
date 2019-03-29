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
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.service.monitoring.MonitoringService;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.matcher.service.crawler.msg.ResourceCrawlUriMessage;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * Camel actor represents the need consumer protocol to a won node. It is used
 * to listen at the WoN node for events about new created needs, activated needs
 * and deactivated needs. Depending on the endpoint it might be used with
 * different protocols (e.g. JMS over activemq). User: hfriedrich Date:
 * 28.04.2015
 */
@Component
@Scope("prototype")
public class NeedConsumerProtocolActor extends UntypedConsumerActor {
    private static final String MSG_HEADER_METHODNAME = "methodName";
    private static final String MSG_HEADER_METHODNAME_NEEDCREATED = "needCreated";
    private static final String MSG_HEADER_METHODNAME_NEEDMODIFIED = "needModified";
    private static final String MSG_HEADER_METHODNAME_NEEDACTIVATED = "needActivated";
    private static final String MSG_HEADER_METHODNAME_NEEDDEACTIVATED = "needDeactivated";
    private static final String MSG_HEADER_WON_NODE_URI = "wonNodeURI";
    private static final String MSG_HEADER_NEED_URI = "needURI";
    private final String endpoint;
    private ActorRef pubSubMediator;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    @Autowired
    private MonitoringService monitoringService;
    @Autowired
    private LinkedDataSource linkedDataSource;

    public NeedConsumerProtocolActor(String endpoint) {
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
            String needUri = (String) camelMsg.getHeaders().get(MSG_HEADER_NEED_URI);
            String wonNodeUri = (String) camelMsg.getHeaders().get(MSG_HEADER_WON_NODE_URI);
            // monitoring code
            monitoringService.startClock(MonitoringService.NEED_HINT_STOPWATCH, needUri);
            // process the incoming need event
            if (needUri != null && wonNodeUri != null) {
                Object methodName = camelMsg.getHeaders().get(MSG_HEADER_METHODNAME);
                if (methodName != null) {
                    log.debug("Received event '{}' for needUri '{}' and wonNeedUri '{}' and publish it to matchers",
                                    methodName, needUri, wonNodeUri);
                    // publish a need event to all the (distributed) matchers
                    NeedEvent event = null;
                    long crawlDate = System.currentTimeMillis();
                    Dataset ds = linkedDataSource.getDataForResource(URI.create(needUri));
                    if (NeedModelWrapper.isANeed(ds)) {
                        if (methodName.equals(MSG_HEADER_METHODNAME_NEEDCREATED)) {
                            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.ACTIVE, crawlDate, ds);
                            pubSubMediator.tell(
                                            new DistributedPubSubMediator.Publish(event.getClass().getName(), event),
                                            getSelf());
                        } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDMODIFIED)) {
                            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.ACTIVE, crawlDate, ds);
                            pubSubMediator.tell(
                                            new DistributedPubSubMediator.Publish(event.getClass().getName(), event),
                                            getSelf());
                        } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDACTIVATED)) {
                            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.ACTIVE, crawlDate, ds);
                            pubSubMediator.tell(
                                            new DistributedPubSubMediator.Publish(event.getClass().getName(), event),
                                            getSelf());
                        } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDDEACTIVATED)) {
                            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.INACTIVE, crawlDate, ds);
                            pubSubMediator.tell(
                                            new DistributedPubSubMediator.Publish(event.getClass().getName(), event),
                                            getSelf());
                        } else {
                            unhandled(message);
                        }
                        // let the crawler save the data of this event too
                        ResourceCrawlUriMessage resMsg = new ResourceCrawlUriMessage(needUri, needUri, wonNodeUri,
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
                log.warning("Message not processed; needURI or wonNodeURI is null");
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
