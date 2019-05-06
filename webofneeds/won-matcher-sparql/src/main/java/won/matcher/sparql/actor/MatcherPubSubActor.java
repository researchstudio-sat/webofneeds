package won.matcher.sparql.actor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.Option;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.BulkAtomEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.event.LoadAtomEvent;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.sparql.config.SparqlMatcherConfig;

/**
 * Created by hfriedrich on 30.09.2015. Matcher actor that subscribes itself to
 * the PubSub Topic to receive atom events from the matching service and
 * forwards them to the actual matcher implementation (e.g. SolrMatcherActor)
 * for hint generation. Then gets back the hints from the matcher implementation
 * and publishes them to the PubSub Topic of hints.
 */
@Component
@Scope("prototype")
public class MatcherPubSubActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef pubSubMediator;
    private ActorRef matcherActor;
    @Autowired
    private SparqlMatcherConfig config;
    private static final String TICK = "tick";
    private static final String APP_STATE_PROPERTIES_FILE_NAME = "state.config.properties";
    private static final String LAST_SEEN_ATOM_DATE_PROPERTY_NAME = "lastSeenAtomDate";
    private boolean atomsUpdateRequestReceived = false;
    private Properties appStateProps = new Properties();
    private Optional<Cancellable> scheduledTick = Optional.empty();

    @Override
    public void preStart() throws IOException {
        // subscribe to atom events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(AtomEvent.class.getName(), getSelf()), getSelf());
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(BulkAtomEvent.class.getName(), getSelf()), getSelf());
        // create the querying and indexing actors that do the actual work
        matcherActor = getContext().actorOf(SpringExtension.SpringExtProvider.get(getContext().system())
                        .fromConfigProps(SparqlMatcherActor.class), "SparqlMatcherPool");
        // Create a scheduler to request missing atom events from matching service while
        // this matcher was not available
        scheduledTick = Optional.of(getContext().system().scheduler().schedule(Duration.create(30, TimeUnit.SECONDS),
                        Duration.create(60, TimeUnit.SECONDS), getSelf(), TICK, getContext().dispatcher(), null));
        // read properties file that has the lastSeenAtomDate
        FileInputStream in = null;
        try {
            in = new FileInputStream(APP_STATE_PROPERTIES_FILE_NAME);
            appStateProps.load(in);
            log.info("loaded properties file {}, property '{}' is set to "
                            + appStateProps.getProperty(LAST_SEEN_ATOM_DATE_PROPERTY_NAME),
                            APP_STATE_PROPERTIES_FILE_NAME, LAST_SEEN_ATOM_DATE_PROPERTY_NAME);
        } catch (FileNotFoundException e) {
            log.info("properties file {} not found, create file", APP_STATE_PROPERTIES_FILE_NAME);
        } catch (IOException e) {
            log.error("cannot read properties file {}", APP_STATE_PROPERTIES_FILE_NAME);
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (appStateProps.getProperty(LAST_SEEN_ATOM_DATE_PROPERTY_NAME) == null) {
                appStateProps.setProperty(LAST_SEEN_ATOM_DATE_PROPERTY_NAME, String.valueOf(-1));
                saveLastSeenAtomDate();
            }
        }
    }

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        if (matcherActor != null) {
            matcherActor.tell(PoisonPill.getInstance(), getSelf());
        }
        cancelScheduledTick();
    }

    @Override
    public void postStop() throws Exception {
        if (matcherActor != null) {
            matcherActor.tell(PoisonPill.getInstance(), getSelf());
        }
        cancelScheduledTick();
    }

    private void cancelScheduledTick() {
        if (scheduledTick.isPresent()) {
            scheduledTick.get().cancel();
        }
    }

    public void saveLastSeenAtomDate() throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(APP_STATE_PROPERTIES_FILE_NAME);
            appStateProps.store(out, null);
        } catch (IOException e) {
            log.error("cannot write properties file {}", APP_STATE_PROPERTIES_FILE_NAME);
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o.equals(TICK)) {
            if (!atomsUpdateRequestReceived) {
                // request missing atom events from matching service while this matcher was not
                // available
                long lastSeenAtomDate = Long.valueOf(appStateProps.getProperty(LAST_SEEN_ATOM_DATE_PROPERTY_NAME));
                LoadAtomEvent loadAtomEvent;
                if (lastSeenAtomDate == -1) {
                    // request the last one atom event from matching service and accept every atom
                    // event timestamp
                    loadAtomEvent = new LoadAtomEvent(1);
                } else {
                    // request atom events with date > last atom event date
                    log.info("request missed atoms from matching service with crawl date > {}", lastSeenAtomDate);
                    loadAtomEvent = new LoadAtomEvent(lastSeenAtomDate, Long.MAX_VALUE);
                }
                pubSubMediator.tell(new DistributedPubSubMediator.Publish(loadAtomEvent.getClass().getName(),
                                loadAtomEvent), getSelf());
            }
        } else if (o instanceof AtomEvent) {
            AtomEvent atomEvent = (AtomEvent) o;
            log.info("AtomEvent received: " + atomEvent);
            // save the last seen atom date property after the atoms are up to date with the
            // matching service
            if (atomsUpdateRequestReceived) {
                long lastSeenAtomDate = Long.valueOf(appStateProps.getProperty(LAST_SEEN_ATOM_DATE_PROPERTY_NAME));
                if (atomEvent.getCrawlDate() > lastSeenAtomDate) {
                    appStateProps.setProperty(LAST_SEEN_ATOM_DATE_PROPERTY_NAME,
                                    String.valueOf(atomEvent.getCrawlDate()));
                    saveLastSeenAtomDate();
                }
            }
            matcherActor.tell(atomEvent, getSelf());
        } else if (o instanceof BulkAtomEvent) {
            // receiving a bulk atom event means this is the answer for the request of atom
            // updates
            // there could arrive several of these bulk events
            atomsUpdateRequestReceived = true;
            BulkAtomEvent bulkAtomEvent = (BulkAtomEvent) o;
            log.info("BulkAtomEvent received with {} atom events", bulkAtomEvent.getAtomEvents().size());
            for (AtomEvent atomEvent : ((BulkAtomEvent) o).getAtomEvents()) {
                long lastSeenAtomDate = Long.valueOf(appStateProps.getProperty(LAST_SEEN_ATOM_DATE_PROPERTY_NAME));
                if (atomEvent.getCrawlDate() > lastSeenAtomDate) {
                    appStateProps.setProperty(LAST_SEEN_ATOM_DATE_PROPERTY_NAME,
                                    String.valueOf(atomEvent.getCrawlDate()));
                    saveLastSeenAtomDate();
                }
                matcherActor.tell(atomEvent, getSelf());
            }
        } else if (o instanceof HintEvent) {
            HintEvent hintEvent = (HintEvent) o;
            log.info("Publish hint event: " + hintEvent);
            pubSubMediator.tell(new DistributedPubSubMediator.Publish(hintEvent.getClass().getName(), hintEvent),
                            getSelf());
        } else if (o instanceof BulkHintEvent) {
            BulkHintEvent bulkHintEvent = (BulkHintEvent) o;
            log.info("Publish bulk hint event: " + bulkHintEvent);
            pubSubMediator.tell(
                            new DistributedPubSubMediator.Publish(bulkHintEvent.getClass().getName(), bulkHintEvent),
                            getSelf());
        } else {
            unhandled(o);
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
