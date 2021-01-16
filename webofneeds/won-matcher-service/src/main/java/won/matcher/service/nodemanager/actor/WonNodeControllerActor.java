package won.matcher.service.nodemanager.actor;

import akka.actor.*;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import org.apache.commons.collections.IteratorUtils;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;
import won.cryptography.service.RegistrationClient;
import won.cryptography.ssl.MessagingContext;
import won.matcher.service.common.event.*;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.service.crawler.actor.MasterCrawlerActor;
import won.matcher.service.nodemanager.config.ActiveMqWonNodeConnectionFactory;
import won.matcher.service.nodemanager.config.WonNodeControllerConfig;
import won.matcher.service.nodemanager.pojo.WonNodeConnection;
import won.matcher.service.nodemanager.service.HintDBService;
import won.matcher.service.nodemanager.service.WonNodeSparqlService;
import won.protocol.service.WonNodeInfo;
import won.protocol.util.RdfUtils.Pair;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Actor that knows all won nodes the matching service is communicating with. It
 * gets informed about new won nodes over the event stream (e.g. by he crawler)
 * and decides which won nodes to crawl and to register with for receiving atom
 * events. There should only exist a single instance of this actor that has the
 * global view of all connected won nodes.
 * <p>
 * User: hfriedrich Date: 27.04.2015
 */
@Component
@Scope("prototype")
public class WonNodeControllerActor extends UntypedActor {
    private static final String LIFE_CHECK_TICK = "life_check_tick";
    @Autowired
    LinkedDataSource linkedDataSource;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef pubSubMediator;
    private ActorRef crawler;
    private ActorRef saveAtomActor;
    private Map<String, WonNodeConnection> crawlWonNodes = new HashMap<>();
    private Set<String> skipWonNodeUris = new HashSet<>();
    private Set<String> failedWonNodeUris = new HashSet<>();
    @Autowired
    private WonNodeSparqlService sparqlService;
    @Autowired
    private WonNodeControllerConfig config;
    @Autowired
    private RegistrationClient registrationClient;
    @Autowired
    private MessagingContext messagingContext;
    @Autowired
    private HintDBService hintDatabase;

    @Override
    public void preStart() {
        // Create a scheduler to execute the life check for each won node regularly
        getContext().system().scheduler().schedule(config.getLifeCheckDuration(), config.getLifeCheckDuration(),
                        getSelf(), LIFE_CHECK_TICK, getContext().dispatcher(), null);
        // Subscribe for won node events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(WonNodeEvent.class.getName(), getSelf()),
                        getSelf());
        // Subscribe for hint events
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(HintEvent.class.getName(), getSelf()), getSelf());
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(BulkHintEvent.class.getName(), getSelf()),
                        getSelf());
        // set won nodes to skip by configuration
        skipWonNodeUris.addAll(config.getSkipWonNodes());
        // get all known won node uris from RDF store
        Set<WonNodeInfo> wonNodeInfo = new HashSet<>();
        try {
            wonNodeInfo = sparqlService.retrieveAllWonNodeInfo();
        } catch (Exception e) {
            log.error("Error querying SPARQL endpoint {}. SPARQL endpoint must be running at matcher service startup!",
                            sparqlService.getSparqlEndpoint());
            log.error("Exception was: {}", e);
            log.info("Shut down matcher service!");
            System.exit(-1);
        }
        // Treat the known won nodes as newly discovered won nodes to register them
        // again at startup of matcher service
        for (WonNodeInfo nodeInfo : wonNodeInfo) {
            if (!config.getCrawlWonNodes().contains(nodeInfo.getWonNodeURI())) {
                WonNodeEvent e = new WonNodeEvent(nodeInfo.getWonNodeURI(),
                                WonNodeEvent.STATUS.NEW_WON_NODE_DISCOVERED);
                pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
            }
        }
        // initialize the won nodes from the config file to crawl
        for (String nodeUri : config.getCrawlWonNodes()) {
            if (!skipWonNodeUris.contains(nodeUri)) {
                if (!crawlWonNodes.containsKey(nodeUri)) {
                    WonNodeEvent e = new WonNodeEvent(nodeUri, WonNodeEvent.STATUS.NEW_WON_NODE_DISCOVERED);
                    pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
                }
            }
        }
        // initialize the crawler
        crawler = getContext().actorOf(
                        SpringExtension.SpringExtProvider.get(getContext().system()).props(MasterCrawlerActor.class),
                        "MasterCrawlerActor");
        // initialize the atom event save actor
        saveAtomActor = getContext().actorOf(
                        SpringExtension.SpringExtProvider.get(getContext().system()).props(SaveAtomEventActor.class),
                        "SaveAtomEventActor");
    }

    /**
     * Receive messages about newly discovered won node and decide to crawl or skip
     * processing these won nodes.
     *
     * @param message
     */
    @Override
    public void onReceive(final Object message) {
        if (message instanceof Terminated) {
            // if it is some other actor handle it differently
            handleConnectionErrors((Terminated) message);
            return;
        }
        if (message.equals(LIFE_CHECK_TICK)) {
            lifeCheck();
            return;
        }
        if (message instanceof WonNodeEvent) {
            WonNodeEvent event = (WonNodeEvent) message;
            if (event.getStatus().equals(WonNodeEvent.STATUS.NEW_WON_NODE_DISCOVERED)
                            || event.getStatus().equals(WonNodeEvent.STATUS.GET_WON_NODE_INFO_FOR_CRAWLING)
                            || event.getStatus().equals(WonNodeEvent.STATUS.RETRY_REGISTER_FAILED_WON_NODE)) {
                // won node has already been discovered and connected
                if (crawlWonNodes.containsKey(event.getWonNodeUri())) {
                    log.debug("Won node uri '{}' already discovered", event.getWonNodeUri());
                    if (event.getStatus().equals(WonNodeEvent.STATUS.GET_WON_NODE_INFO_FOR_CRAWLING)) {
                        WonNodeInfo wonNodeInfo = crawlWonNodes.get(event.getWonNodeUri()).getWonNodeInfo();
                        WonNodeEvent e = new WonNodeEvent(event.getWonNodeUri(),
                                        WonNodeEvent.STATUS.CONNECTED_TO_WON_NODE, wonNodeInfo);
                        pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e),
                                        getSelf());
                    }
                    return;
                }
                // skip crawling of won nodes in the skip list
                if (skipWonNodeUris.contains(event.getWonNodeUri())) {
                    log.debug("Skip crawling won node with uri '{}'", event.getWonNodeUri());
                    WonNodeEvent e = new WonNodeEvent(event.getWonNodeUri(), WonNodeEvent.STATUS.SKIP_WON_NODE);
                    pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
                    return;
                }
                // shall we try to connect to the won node or has it failed already ?
                if (failedWonNodeUris.contains(event.getWonNodeUri())) {
                    log.debug("Suppress connection to already failed won node with uri {} , will try to connect later ...",
                                    event.getWonNodeUri());
                    return;
                }
                // try the connect to won node
                boolean logRegisterWarningForWonNode = event.getStatus()
                                .equals(WonNodeEvent.STATUS.RETRY_REGISTER_FAILED_WON_NODE);
                WonNodeConnection wonNodeConnection = addWonNodeForCrawling(event.getWonNodeUri(),
                                logRegisterWarningForWonNode);
                // connection failed ?
                if (failedWonNodeUris.contains(event.getWonNodeUri())) {
                    log.debug("Still could not connect to won node with uri: {}, will retry later ...",
                                    event.getWonNodeUri());
                    return;
                }
                // tell the crawler about discovered won nodes
                if (wonNodeConnection == null || wonNodeConnection.getWonNodeInfo() == null) {
                    log.error("Cannot retrieve won node info from won node connection!");
                    return;
                }
                WonNodeEvent e = new WonNodeEvent(event.getWonNodeUri(), WonNodeEvent.STATUS.CONNECTED_TO_WON_NODE,
                                wonNodeConnection.getWonNodeInfo());
                pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
                return;
            }
        }
        // send back hints to won nodes
        if (message instanceof HintEvent) {
            processHint((HintEvent) message);
            return;
        } else if (message instanceof BulkHintEvent) {
            BulkHintEvent bulkHintEvent = (BulkHintEvent) message;
            for (HintEvent hint : bulkHintEvent.getHintEvents()) {
                expandToSocketHintsIfAppropriate(hint)
                                .forEach(h -> processHint(h));
            }
            return;
        }
        unhandled(message);
    }

    private void processHint(HintEvent hint) {
        // hint duplicate filter
        if (hintDatabase.mightHintSaved(hint)) {
            log.debug("Hint " + hint + " is filtered out by duplicate filter!");
            hintDatabase.saveHint(hint);
            return;
        }
        // save the hint and send it to the won node controller which sends it to the
        // responsible won node
        hintDatabase.saveHint(hint);
        sendHint(hint);
    }

    /**
     * Send hint event out to won node
     *
     * @param hint
     */
    private void sendHint(HintEvent hint) {
        if (!crawlWonNodes.containsKey(hint.getRecipientWonNodeUri())) {
            log.warning("cannot send hint to won node {}! Is registered with the won node controller?",
                            hint.getRecipientWonNodeUri());
            return;
        }
        // send hint to first won node
        WonNodeConnection fromWonNodeConnection = crawlWonNodes.get(hint.getRecipientWonNodeUri());
        log.info("Send hint {} to won node {}", hint, hint.getRecipientWonNodeUri());
        fromWonNodeConnection.getHintProducer().tell(hint, getSelf());
    }

    /**
     * If the HintEvent is an AtomHintEvent, this method checks the respective atoms
     * for compatible sockets and generates one SocketHintEvent for each
     * combination. If no such combinations are found, or if the hint is not an
     * AtomHintEvent, the original hint is returned as the only element in the
     * collection.
     * 
     * @param message
     * @return
     */
    private Collection<HintEvent> expandToSocketHintsIfAppropriate(HintEvent message) {
        if (message instanceof AtomHintEvent) {
            AtomHintEvent ahe = (AtomHintEvent) message;
            Set<Pair<URI>> compatibleSocketPairs = WonLinkedDataUtils.getCompatibleSocketsForAtoms(linkedDataSource,
                            URI.create(ahe.getRecipientAtomUri()),
                            URI.create(ahe.getTargetAtomUri()));
            if (!compatibleSocketPairs.isEmpty()) {
                return compatibleSocketPairs
                                .stream()
                                .map(p -> new SocketHintEvent(
                                                p.getFirst().toString(),
                                                ahe.getRecipientWonNodeUri(),
                                                p.getSecond().toString(),
                                                ahe.getTargetWonNodeUri(),
                                                ahe.getMatcherUri(),
                                                ahe.getScore(),
                                                ahe.getCause()))
                                .collect(Collectors.toList());
            }
        }
        return Collections.singletonList(message);
    }

    /**
     * Try to register at won nodes and add them for crawling
     *
     * @param wonNodeUri URI of the won node meta data resource
     * @param logWonNodeRegisterWarning if true then log the failed register
     * attempts as warning, otherwise as debug level
     * @return won node connection if successfully connected, otherwise null
     */
    private WonNodeConnection addWonNodeForCrawling(String wonNodeUri, boolean logWonNodeRegisterWarning) {
        WonNodeConnection con = null;
        Dataset ds = null;
        WonNodeInfo nodeInfo = null;
        // try register at won node
        try {
            registrationClient.register(wonNodeUri);
            ds = linkedDataSource.getDataForResource(URI.create(wonNodeUri));
        } catch (Exception e) {
            addFailedWonNode(wonNodeUri, con);
            if (logWonNodeRegisterWarning) {
                log.warning("Error requesting won node information from {}", wonNodeUri);
                log.warning("Exception message: {} \nCause: {} ", e.getMessage(), e.getCause());
            } else {
                log.debug("Error requesting won node information from {}", wonNodeUri);
                log.debug("Exception message: {} \nCause: {} ", e.getMessage(), e.getCause());
            }
            return null;
        }
        // try save won node info in local rdf store
        try {
            sparqlService.updateNamedGraphsOfDataset(ds);
            nodeInfo = sparqlService.getWonNodeInfoFromDataset(ds);
        } catch (Exception e) {
            addFailedWonNode(wonNodeUri, con);
            log.error("Error saving won node information from {} into RDF store with SPARQL endpoint {}", wonNodeUri,
                            sparqlService.getSparqlEndpoint());
            log.error("Exception message: {} \nCause: {} ", e.getMessage(), e.getCause());
            return null;
        }
        // try subscribe atom updates at won node
        try {
            con = subscribeAtomUpdates(nodeInfo);
            crawlWonNodes.put(nodeInfo.getWonNodeURI(), con);
            failedWonNodeUris.remove(nodeInfo.getWonNodeURI());
            log.info("registered won node {} and start crawling it", nodeInfo.getWonNodeURI());
        } catch (Exception e) {
            addFailedWonNode(wonNodeUri, con);
            log.error("Error subscribing for atom updates at won node {}", wonNodeUri);
            log.error("Exception message: {} \nCause: {} ", e.getMessage(), e.getCause());
        }
        return con;
    }

    /**
     * Try to connect to unreachable won nodes from time to time
     */
    private void lifeCheck() {
        List<String> failedNodes = IteratorUtils.toList(failedWonNodeUris.iterator());
        log.debug("retry to connect to all failed won nodes again: {}", failedNodes);
        failedWonNodeUris.clear();
        for (String uri : failedNodes) {
            // try register at the wonnode again
            WonNodeEvent e = new WonNodeEvent(uri, WonNodeEvent.STATUS.RETRY_REGISTER_FAILED_WON_NODE);
            pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
        }
    }

    /**
     * Add a won node to the failed list and stop all its consumers
     *
     * @param wonNodeUri
     * @param con
     */
    private void addFailedWonNode(String wonNodeUri, WonNodeConnection con) {
        if (con != null) {
            getContext().stop(con.getAtomCreatedConsumer());
            getContext().stop(con.getAtomActivatedConsumer());
            getContext().stop(con.getAtomDeactivatedConsumer());
        }
        crawlWonNodes.remove(wonNodeUri);
        failedWonNodeUris.add(wonNodeUri);
    }

    private WonNodeConnection subscribeAtomUpdates(WonNodeInfo wonNodeInfo) {
        return ActiveMqWonNodeConnectionFactory.createWonNodeConnection(getContext(), wonNodeInfo, messagingContext);
    }

    /**
     * Handles connections errors that occur when the atom consumer actors are
     * terminated.
     *
     * @param t messages that holds a reference to consumer actor that was
     * terminated
     */
    private void handleConnectionErrors(Terminated t) {
        for (String uri : crawlWonNodes.keySet()) {
            WonNodeConnection con = crawlWonNodes.get(uri);
            if (con != null) {
                if (con.getAtomCreatedConsumer().equals(t.getActor())) {
                    log.error("AtomCreatedConsumer '{}' of won '{}' has been shut down", t.getActor(), uri);
                    addFailedWonNode(con.getWonNodeInfo().getWonNodeURI(), con);
                } else if (con.getAtomActivatedConsumer().equals(t.getActor())) {
                    log.error("AtomActivatedConsumer '{}' of won '{}' has been shut down", t.getActor(), uri);
                    addFailedWonNode(con.getWonNodeInfo().getWonNodeURI(), con);
                } else if (con.getAtomDeactivatedConsumer().equals(t.getActor())) {
                    log.error("AtomDeactivatedConsumer '{}' of won '{}' has been shut down", t.getActor(), uri);
                    addFailedWonNode(con.getWonNodeInfo().getWonNodeURI(), con);
                } else if (con.getHintProducer().equals(t.getActor())) {
                    log.error("HintProducer '{}' of won '{}' has been shut down", t.getActor(), uri);
                    addFailedWonNode(con.getWonNodeInfo().getWonNodeURI(), con);
                }
            }
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
