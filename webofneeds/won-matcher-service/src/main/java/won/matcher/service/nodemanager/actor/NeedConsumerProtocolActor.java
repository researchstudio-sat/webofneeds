package won.matcher.service.nodemanager.actor;

import akka.actor.ActorRef;
import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedConsumerActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.service.monitoring.MonitoringService;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Camel actor represents the need consumer protocol to a won node.
 * It is used to listen at the WoN node for events about new created needs,
 * activated needs and deactivated needs. Depending on the endpoint it
 * might be used with different protocols (e.g. JMS over activemq).
 *
 * User: hfriedrich
 * Date: 28.04.2015
 */
@Component
@Scope("prototype")
public class NeedConsumerProtocolActor extends UntypedConsumerActor
{
  private static final String MSG_HEADER_METHODNAME = "methodName";
  private static final String MSG_HEADER_METHODNAME_NEEDCREATED = "needCreated";
  private static final String MSG_HEADER_METHODNAME_NEEDACTIVATED = "needActivated";
  private static final String MSG_HEADER_METHODNAME_NEEDDEACTIVATED = "needDeactivated";
  private static final String MSG_HEADER_WON_NODE_URI = "wonNodeURI";
  private static final String MSG_HEADER_NEED_URI = "needUri";
  private final String endpoint;
  private ActorRef pubSubMediator;
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  @Autowired
  private MonitoringService monitoringService;

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
      if (monitoringService.isMonitoringEnabled()) {
        monitoringService.startClock(MonitoringService.NEED_HINT_STOPWATCH, needUri);
      }

      // process the incoming need event
      if (needUri != null && wonNodeUri != null) {
        Object methodName = camelMsg.getHeaders().get(MSG_HEADER_METHODNAME);
        if (methodName != null) {
          log.debug("Received event '{}' for needUri '{}' and wonNeedUri '{}'", methodName, needUri, wonNodeUri);

          // publish an internal need event
          NeedEvent event = null;
          if (methodName.equals(MSG_HEADER_METHODNAME_NEEDCREATED)) {
            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.CREATED, camelMsg.body().toString(), Lang.TRIG);
            pubSubMediator.tell(new DistributedPubSubMediator.Publish(event.getClass().getName(), event), getSelf());
            return;
          } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDACTIVATED)) {
            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.ACTIVATED, camelMsg.body().toString(), Lang.TRIG);
            pubSubMediator.tell(new DistributedPubSubMediator.Publish(event.getClass().getName(), event), getSelf());
            return;
          } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDDEACTIVATED)) {
            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.DEACTIVATED, camelMsg.body().toString(), Lang.TRIG);
            pubSubMediator.tell(new DistributedPubSubMediator.Publish(event.getClass().getName(), event), getSelf());
            return;
          }
        }
      }
    } else {
      System.out.print("some other message");
    }
    unhandled(message);
  }

}
