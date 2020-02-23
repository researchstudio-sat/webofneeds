package won.matcher.service.rematch.actor;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.event.AtomHintEvent;
import won.matcher.service.common.event.BulkAtomEvent;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.Cause;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.event.SocketHintEvent;
import won.matcher.service.rematch.config.RematchConfig;
import won.matcher.service.rematch.service.RematchSparqlService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Actor that is responsible for re-matching and inverse matching. Re-matching
 * means repeatedly checking for matches for a given atom. Inverse matching
 * means matching for an atom that was found as a match for another atom. The
 * actor subscribes for AtomEvents and HintEvents. AtomEvents are those events
 * that indicate that we want to match for a given atom. This may happen because
 * the atom has just been created or updated, or because the crawler found it.
 * HintEvents are generated by the matchers, containing all the information
 * about a match they found. The RematchActor keeps track of the time when we
 * last tried to find matches for a given atom (i.e., the last AtomEvent it has
 * seen). It keeps track of this to avoid matching too frequently for any Atom.
 * The RematchActor starts a scheduler on startup that generates a 'rematch
 * tick' at fixed intervals. Every time the 'rematch tick' is generated, the
 * RematchActor checks which Atoms require re-matching. A BulkAtomEvent is
 * generated for all those atoms, which the matchers will react to. Every time a
 * HintEvent is seen by the RematchActor, an AtomEvent is generated for the hint
 * object, causing the matchers to search for inverse matches. Note that the
 * RematchActor may HintEvent.getCause() and AtomEvent.getCause() in its
 * rematching strategy, so different outcomes should be expected for different
 * Cause values.
 * 
 * @author fkleedorfer
 */
@Component
@Scope("prototype")
public class RematchActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private static final String REMATCH_TICK = "rematch_tick";
    private ActorRef pubSubMediator;
    @Autowired
    private RematchSparqlService rematchSparqlService;
    @Autowired
    RematchConfig config;
    @Autowired
    LinkedDataSource linkedDataSource;

    public void setConfig(RematchConfig config) {
        this.config = config;
    }

    @Override
    public void preStart() {
        // Create a scheduler to execute the life check for each won node regularly
        getContext().system().scheduler().schedule(config.getRematchInterval(), config.getRematchInterval(), getSelf(),
                        REMATCH_TICK, getContext().dispatcher(), null);
        // Subscribe for won node events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        // Subscribe for hint events and atom events
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(HintEvent.class.getName(), getSelf()), getSelf());
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(BulkHintEvent.class.getName(), getSelf()),
                        getSelf());
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(AtomEvent.class.getName(), getSelf()), getSelf());
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(BulkAtomEvent.class.getName(), getSelf()),
                        getSelf());
        log.debug("RematchActor startup complete");
    }

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof HintEvent) {
            handleHintEvent((HintEvent) msg);
            return;
        } else if (msg instanceof BulkHintEvent) {
            handleBulkHintEvent((BulkHintEvent) msg);
            return;
        } else if (msg instanceof AtomEvent) {
            handleAtomEvent((AtomEvent) msg);
            return;
        } else if (msg instanceof BulkAtomEvent) {
            handleBulkAtomEvent((BulkAtomEvent) msg);
            return;
        } else if (msg.equals(REMATCH_TICK)) {
            rematch();
            return;
        } else {
            unhandled(msg);
        }
    }

    private void rematch() {
        Set<BulkAtomEvent> rematchBulks = rematchSparqlService.findAtomsForRematching();
        if (!rematchBulks.isEmpty()) {
            rematchBulks.stream().forEach(rematchEvent -> {
                pubSubMediator.tell(
                                new DistributedPubSubMediator.Publish(rematchEvent.getClass().getName(), rematchEvent),
                                getSelf());
            });
        }
        if (log.isDebugEnabled()) {
            int cnt = rematchBulks.stream().map(b -> b.getAtomEvents().size()).reduce(0, (a, b) -> a + b);
            log.debug("Found {} atoms for rematching", cnt);
        }
    }

    private void handleAtomEvent(AtomEvent msg) {
        rematchSparqlService.registerMatchingAttempt(msg);
        if (log.isDebugEnabled()) {
            log.debug("Handled AtomEvent: " + msg.getUri(), ", cause: " + msg.getCause());
        }
    }

    private void handleBulkAtomEvent(BulkAtomEvent msg) {
        rematchSparqlService.registerMatchingAttempts(msg);
        if (log.isDebugEnabled()) {
            log.debug("Handled BulkAtomEvent of size " + msg.getAtomEvents().size());
        }
    }

    private void handleBulkHintEvent(BulkHintEvent msg) {
        BulkAtomEvent bulkAtomEvent = new BulkAtomEvent();
        msg.getHintEvents().stream().map(m -> processHint(m))
                        .forEach(ae -> ae.ifPresent(x -> bulkAtomEvent.addAtomEvent(x)));
        if (!bulkAtomEvent.getAtomEvents().isEmpty()) {
            pubSubMediator.tell(
                            new DistributedPubSubMediator.Publish(bulkAtomEvent.getClass().getName(), bulkAtomEvent),
                            getSelf());
        }
        if (log.isDebugEnabled()) {
            log.debug("Handled BulkHintEvent of size " + msg.getHintEvents().size());
        }
    }

    private void handleHintEvent(HintEvent msg) {
        processHint(msg).ifPresent(e -> pubSubMediator
                        .tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf()));
        if (log.isDebugEnabled()) {
            log.debug("Handled HintEvent: " + msg);
        }
    }

    private Optional<AtomEvent> processHint(HintEvent msg) {
        if (msg.getCause() == Cause.MATCHED) {
            // don't do inverse matching for results of inverse matching
            return Optional.empty();
        }
        Optional<URI> targetAtom = null;
        String targetWonNode = msg.getTargetWonNodeUri();
        if (msg instanceof SocketHintEvent) {
            targetAtom = WonLinkedDataUtils.getAtomOfSocket(URI.create(((SocketHintEvent) msg).getTargetSocketUri()),
                            linkedDataSource);
        } else if (msg instanceof AtomHintEvent) {
            targetAtom = Optional.of(URI.create(((AtomHintEvent) msg).getTargetAtomUri()));
        }
        if (!targetAtom.isPresent()) {
            return Optional.empty();
        }
        Dataset ds = linkedDataSource.getDataForResource(targetAtom.get());
        return Optional.of(new AtomEvent(targetAtom.get().toString(), targetWonNode, AtomEvent.TYPE.ACTIVE,
                        System.currentTimeMillis(), ds, Cause.MATCHED));
    }
}
