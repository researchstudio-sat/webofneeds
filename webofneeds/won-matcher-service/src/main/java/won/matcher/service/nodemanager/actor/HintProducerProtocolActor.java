package won.matcher.service.nodemanager.actor;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedProducerActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.service.monitoring.MonitoringService;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageEncoder;

/**
 * Akka camel actor used to send out hints to won nodes Created by hfriedrich on
 * 27.08.2015.
 */
@Component
@Scope("prototype")
public class HintProducerProtocolActor extends UntypedProducerActor {
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
        headers.put("atomURI", hint.getFromAtomUri());
        headers.put("otherAtomURI", hint.getToAtomUri());
        headers.put("score", String.valueOf(hint.getScore()));
        headers.put("originator", hint.getMatcherUri());
        // headers.put("content",
        // RdfUtils.toString(hint.deserializeExplanationModel()));
        // headers.put("remoteBrokerEndpoint", localBrokerUri);
        headers.put("methodName", "hint");
        WonMessage wonMessage = createHintWonMessage(hint);
        Object body = WonMessageEncoder.encode(wonMessage, Lang.TRIG);
        CamelMessage camelMsg = new CamelMessage(body, headers);
        // monitoring code
        monitoringService.stopClock(MonitoringService.ATOM_HINT_STOPWATCH, hint.getFromAtomUri());
        log.debug("Send hint camel message {}", hint.getFromAtomUri());
        return camelMsg;
    }

    /**
     * create a won message out of an hint event
     *
     * @param hint
     * @return
     * @throws WonMessageBuilderException
     */
    private WonMessage createHintWonMessage(HintEvent hint) throws WonMessageBuilderException {
        URI wonNode = URI.create(hint.getFromWonNodeUri());
        return WonMessageBuilder
                        .setMessagePropertiesForHint(hint.getGeneratedEventUri(), URI.create(hint.getFromAtomUri()),
                                        Optional.empty(), wonNode, URI.create(hint.getToAtomUri()), Optional.empty(),
                                        URI.create(hint.getMatcherUri()), hint.getScore())
                        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL).build();
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
