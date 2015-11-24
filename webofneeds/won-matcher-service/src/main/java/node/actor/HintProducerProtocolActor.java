package node.actor;

import akka.actor.ActorRef;
import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedProducerActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.HintEvent;
import common.service.monitoring.MonitoringService;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.FacetType;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Akka camel actor used to send out hints to won nodes
 *
 * Created by hfriedrich on 27.08.2015.
 */
@Component
@Scope("prototype")
public class HintProducerProtocolActor extends UntypedProducerActor
{
  @Autowired
  private MonitoringService monitoringService;

  private String endpoint;
  private String localBrokerUri;
  private ActorRef pubSubMediator;
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  public HintProducerProtocolActor(String endpoint, String localBrokerUri) {
    this.endpoint = endpoint;
    this.localBrokerUri = localBrokerUri;
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
  }

  @Override
  public String getEndpointUri() {
    return endpoint;
  }

  /**
   * transform hint events to camel messages that can be sent to the won node
   *
   * @param message supposed to be a {@link HintEvent}
   * @return
   */
  @Override
  public Object onTransformOutgoingMessage(Object message) {

    HintEvent hint = (HintEvent) message;
    Map<String, Object> headers = new HashMap<>();
    headers.put("needURI", hint.getFromNeedUri());
    headers.put("otherNeedURI", hint.getToNeedUri());
    headers.put("score", String.valueOf(hint.getScore()));
    headers.put("originator", hint.getMatcherUri());
    //headers.put("content", RdfUtils.toString(hint.deserializeExplanationModel()));
    //headers.put("remoteBrokerEndpoint", localBrokerUri);
    headers.put("methodName", "hint");

    WonMessage wonMessage = createHintWonMessage(hint);
    Object body = WonMessageEncoder.encode(wonMessage, Lang.TRIG);
    CamelMessage camelMsg = new CamelMessage(body, headers);

    // monitoring code
    if (monitoringService.isMonitoringEnabled()) {
      monitoringService.stopClock(MonitoringService.NEED_HINT_STOPWATCH, hint.getFromNeedUri());
    }

    log.debug("Send hint camel message {}", hint.getFromNeedUri());
    return camelMsg;
  }

  /**
   * create a won message out of an hint event
   *
   * @param hint
   * @return
   * @throws WonMessageBuilderException
   */
  private WonMessage createHintWonMessage(HintEvent hint)
    throws WonMessageBuilderException {

    URI wonNode = URI.create(hint.getFromWonNodeUri());
    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
      .setMessagePropertiesForHint(
        hint.getGeneratedEventUri(),
        URI.create(hint.getFromNeedUri()),
        FacetType.OwnerFacet.getURI(),
        wonNode,
        URI.create(hint.getToNeedUri()),
        FacetType.OwnerFacet.getURI(),
        URI.create(hint.getMatcherUri()),
        hint.getScore())
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
      .build();
  }

}
