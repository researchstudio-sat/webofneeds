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
import won.matcher.service.common.event.AtomHintEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.event.SocketHintEvent;
import won.matcher.service.common.service.monitoring.MonitoringService;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.impl.WonMessageSignerVerifier;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Akka camel actor used to send out hints to won nodes Created by hfriedrich on
 * 27.08.2015.
 */
@Component
@Scope("prototype")
public class HintProducerProtocolActor extends UntypedProducerActor {
    @Autowired
    private MonitoringService monitoringService;
    @Autowired
    private WonNodeInformationService wonNodeInformationService;
    @Autowired
    private LinkedDataSource linkedDataSource;
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
        headers.put("methodName", "hint");
        Optional<WonMessage> wonMessage = createHintWonMessage(hint);
        if (wonMessage.isPresent()) {
            WonMessage msg = null;
            try {
                msg = WonMessageSignerVerifier.seal(wonMessage.get());
                Object body = WonMessageEncoder.encode(msg, Lang.TRIG);
                CamelMessage camelMsg = new CamelMessage(body, headers);
                // monitoring code
                stopStopwatch(hint);
                return camelMsg;
            } catch (Exception e) {
                log.warning("Error preparing hint message", e);
            }
        }
        return null;
    }

    private void stopStopwatch(HintEvent hint) {
        Optional<String> stopwatchTag = Optional.empty();
        if (hint instanceof AtomHintEvent) {
            stopwatchTag = Optional.of(((AtomHintEvent) hint).getTargetAtomUri());
        } else if (hint instanceof SocketHintEvent) {
            stopwatchTag = Optional.of(((SocketHintEvent) hint).getTargetSocketUri());
        }
        if (stopwatchTag.isPresent()) {
            monitoringService.stopClock(MonitoringService.ATOM_HINT_STOPWATCH, stopwatchTag.get());
            log.debug("Send hint camel message {}", stopwatchTag.get());
        }
    }

    /**
     * create a won message out of an hint event
     *
     * @param hint
     * @return
     * @throws WonMessageBuilderException
     */
    private Optional<WonMessage> createHintWonMessage(HintEvent hint) throws WonMessageBuilderException {
        if (hint instanceof AtomHintEvent) {
            AtomHintEvent ahe = (AtomHintEvent) hint;
            return Optional.of(WonMessageBuilder
                            .atomHint()
                            .atom(URI.create(ahe.getRecipientAtomUri()))
                            .hintTargetAtom(URI.create(ahe.getTargetAtomUri()))
                            .hintScore(ahe.getScore())
                            .direction().fromExternal()
                            .build());
        } else if (hint instanceof SocketHintEvent) {
            SocketHintEvent she = (SocketHintEvent) hint;
            Optional<URI> recipientAtomURI = WonLinkedDataUtils.getAtomOfSocket(URI.create(she.getRecipientSocketUri()),
                            linkedDataSource);
            if (recipientAtomURI.isPresent()) {
                return Optional.of(WonMessageBuilder
                                .socketHint()
                                .recipientSocket(URI.create(she.getRecipientSocketUri()))
                                .hintTargetSocket(URI.create(she.getTargetSocketUri()))
                                .hintScore(hint.getScore())
                                .direction().fromExternal()
                                .build());
            }
        }
        return Optional.empty();
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
