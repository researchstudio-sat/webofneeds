package won.matcher.solr.actor;

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
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.event.LoadNeedEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.solr.config.SolrMatcherConfig;

/**
 * Created by hfriedrich on 30.09.2015. Matcher actor that subscribes itself to
 * the PubSub Topic to receive need events from the matching service and
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
    private SolrMatcherConfig config;
    private static final String TICK = "tick";
    private static final String APP_STATE_PROPERTIES_FILE_NAME = "state.config.properties";
    private static final String LAST_SEEN_NEED_DATE_PROPERTY_NAME = "lastSeenNeedDate";
    private boolean needsUpdateRequestReceived = false;
    private Properties appStateProps = new Properties();
    private Optional<Cancellable> scheduledTick = Optional.empty();

    @Override
    public void preStart() throws IOException {
        // subscribe to need events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(NeedEvent.class.getName(), getSelf()), getSelf());
        // create the querying and indexing actors that do the actual work
        matcherActor = getContext().actorOf(SpringExtension.SpringExtProvider.get(getContext().system())
                        .fromConfigProps(SolrMatcherActor.class), "SolrMatcherPool");
        // Create a scheduler to request missing need events from matching service while
        // this matcher was not available
        scheduledTick = Optional.of(getContext().system().scheduler().schedule(Duration.create(30, TimeUnit.SECONDS),
                        Duration.create(60, TimeUnit.SECONDS), getSelf(), TICK, getContext().dispatcher(), null));
        // read properties file that has the lastSeenNeedDate
        FileInputStream in = null;
        try {
            in = new FileInputStream(APP_STATE_PROPERTIES_FILE_NAME);
            appStateProps.load(in);
            log.info("loaded properties file {}, property '{}' is set to "
                            + appStateProps.getProperty(LAST_SEEN_NEED_DATE_PROPERTY_NAME),
                            APP_STATE_PROPERTIES_FILE_NAME, LAST_SEEN_NEED_DATE_PROPERTY_NAME);
        } catch (FileNotFoundException e) {
            log.info("properties file {} not found, create file", APP_STATE_PROPERTIES_FILE_NAME);
        } catch (IOException e) {
            log.error("cannot read properties file {}", APP_STATE_PROPERTIES_FILE_NAME);
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (appStateProps.getProperty(LAST_SEEN_NEED_DATE_PROPERTY_NAME) == null) {
                appStateProps.setProperty(LAST_SEEN_NEED_DATE_PROPERTY_NAME, String.valueOf(-1));
                saveLastSeenNeedDate();
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

    public void saveLastSeenNeedDate() throws IOException {
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
            if (!needsUpdateRequestReceived) {
                // request missing need events from matching service while this matcher was not
                // available
                long lastSeenNeedDate = Long.valueOf(appStateProps.getProperty(LAST_SEEN_NEED_DATE_PROPERTY_NAME));
                LoadNeedEvent loadNeedEvent;
                if (lastSeenNeedDate == -1) {
                    // request the last one need event from matching service and accept every need
                    // event timestamp
                    loadNeedEvent = new LoadNeedEvent(1);
                } else {
                    // request need events with date > last need event date
                    log.info("request missed needs from matching service with crawl date > {}", lastSeenNeedDate);
                    loadNeedEvent = new LoadNeedEvent(lastSeenNeedDate, Long.MAX_VALUE);
                }
                pubSubMediator.tell(new DistributedPubSubMediator.Publish(loadNeedEvent.getClass().getName(),
                                loadNeedEvent), getSelf());
            }
        } else if (o instanceof NeedEvent) {
            NeedEvent needEvent = (NeedEvent) o;
            log.info("NeedEvent received: " + needEvent);
            // save the last seen need date property after the needs are up to date with the
            // matching service
            if (needsUpdateRequestReceived) {
                long lastSeenNeedDate = Long.valueOf(appStateProps.getProperty(LAST_SEEN_NEED_DATE_PROPERTY_NAME));
                if (needEvent.getCrawlDate() > lastSeenNeedDate) {
                    appStateProps.setProperty(LAST_SEEN_NEED_DATE_PROPERTY_NAME,
                                    String.valueOf(needEvent.getCrawlDate()));
                    saveLastSeenNeedDate();
                }
            }
            matcherActor.tell(needEvent, getSelf());
        } else if (o instanceof BulkNeedEvent) {
            // receiving a bulk need event means this is the answer for the request of need
            // updates
            // there could arrive several of these bulk events
            needsUpdateRequestReceived = true;
            BulkNeedEvent bulkNeedEvent = (BulkNeedEvent) o;
            log.info("BulkNeedEvent received with {} need events", bulkNeedEvent.getNeedEvents().size());
            for (NeedEvent needEvent : ((BulkNeedEvent) o).getNeedEvents()) {
                long lastSeenNeedDate = Long.valueOf(appStateProps.getProperty(LAST_SEEN_NEED_DATE_PROPERTY_NAME));
                if (needEvent.getCrawlDate() > lastSeenNeedDate) {
                    appStateProps.setProperty(LAST_SEEN_NEED_DATE_PROPERTY_NAME,
                                    String.valueOf(needEvent.getCrawlDate()));
                    saveLastSeenNeedDate();
                }
                matcherActor.tell(needEvent, getSelf());
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
