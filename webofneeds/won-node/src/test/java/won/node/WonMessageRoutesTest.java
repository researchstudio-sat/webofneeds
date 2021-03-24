package won.node;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import won.cryptography.service.RandomNumberService;
import won.node.camel.service.WonCamelHelper;
import won.node.protocol.impl.MessageRoutingInfoServiceWithLookup;
import won.node.service.linkeddata.generate.LinkedDataService;
import won.node.service.linkeddata.lookup.SocketLookupFromLinkedData;
import won.node.service.nodeconfig.URIService;
import won.node.service.persistence.*;
import won.node.springsecurity.acl.WonAclEvalContext;
import won.protocol.exception.WonMessageNotWellFormedException;
import won.protocol.jms.AtomProtocolCommunicationService;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.impl.KeyForNewAtomAddingProcessor;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.message.processor.impl.WonMessageSignerVerifier;
import won.protocol.model.Connection;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.service.WonNodeInfoBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.CheapInsecureRandomString;
import won.protocol.util.Prefixer;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.pretty.Lang_WON;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXCHAT;
import won.test.category.RequiresPostgresServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static won.node.camel.WonNodeConstants.*;
import static won.node.camel.service.WonCamelHelper.*;

/**
 * Tests that check the input/output behaviour of the WoN node.
 * <p>
 * All relevant external dependencies are mocked, as is the registration
 * mechanism. The messages are generated and sent to the camel endpoint at which
 * they normally arrive through an activemq queue.
 * </p>
 * <p>
 * The outgoing messages are intercepted and checked.
 * </p>
 *
 * @author fkleedorferT
 */
@ContextConfiguration(locations = { "classpath:/won/node/WonMessageRoutesTest.xml",
                "classpath:/won/node/WonMessageRoutesTest/jdbc-storage.xml",
                "classpath:/spring/component/storage/jpabased-rdf-storage.xml",
                "classpath:/spring/component/camel/node-camel.xml",
                "classpath:/spring/component/camel/message-processors.xml",
                "classpath:/spring/component/threadpool.xml",
                "classpath:/spring/component/cryptographyServices.xml",
                "classpath:/spring/component/crypto/node-crypto.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource
@Rollback
@DirtiesContext
@Category(RequiresPostgresServer.class)
public abstract class WonMessageRoutesTest {
    protected static final String OWNERAPPLICATION_ID_OWNER1 = "ownerapp-1";
    protected static final String OWNERAPPLICATION_ID_OWNER2 = "ownerapp-2";
    protected static final URI URI_NODE_1 = URI.create("https://localhost:8443/won/resource");
    protected static final URI URI_MATCHER_1 = URI.create("uri:matcher-1");
    protected static final URI URI_ATOM_1_ON_NODE_1 = URI.create("uri:atom-11");
    protected static final URI URI_ATOM_2_ON_NODE_1 = URI.create("uri:atom-12");
    protected static final URI URI_ATOM_1_ON_NODE_2 = URI.create("uri:atom-21");
    protected static final URI URI_ATOM_2_ON_NODE_2 = URI.create("uri:atom-22");
    protected static AtomicInteger counter = new AtomicInteger(0);
    protected static ExecutorService executor;
    static BrokerService brokerSvc;
    protected final CheapInsecureRandomString randomString = new CheapInsecureRandomString();
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Mocked endpoints
     *
     * @throws Exception
     */
    @EndpointInject("mock:direct:OwnerProtocolOut")
    protected MockEndpoint toOwnerMockEndpoint;
    @EndpointInject("mock:seda:MatcherProtocolOut")
    protected MockEndpoint toMatcherMockEndpoint;
    /**
     * Actual services/injects
     */
    @Autowired
    CamelContext camelContext;
    @Autowired
    ProducerTemplate producerTemplate;
    @Autowired
    SignatureAddingWonMessageProcessor signatureAdder;
    @Autowired
    KeyForNewAtomAddingProcessor atomKeyGeneratorAndAdder;
    @Autowired
    MessagingService messagingService;
    @Autowired
    ActiveMQOwnerManagementService activeMqOwnerManagementService;
    @Autowired
    LinkedDataService linkedDataService;
    @Autowired
    AtomService atomService;
    @Autowired
    MessageService messageService;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    SocketRepository socketRepository;
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    ConnectionRepository connectionRepository;
    /**
     * Mocked services
     */
    @MockBean
    SocketLookupFromLinkedData socketLookup;
    @MockBean
    WonNodeInformationService wonNodeInformationService;
    @MockBean
    AtomProtocolCommunicationService atomProtocolCommunicationService;
    @MockBean
    LinkedDataSource linkedDataSource;
    @MockBean
    RandomNumberService RandomNumberService;
    @MockBean
    DataDerivationService dataDerivationService;
    @MockBean
    MessageRoutingInfoServiceWithLookup messageRoutingInfoServiceWithLookup;
    @MockBean
    URIService uriService;
    // @MockBean MessagingContext messagingContext;
    // @MockBean LinkedDataRestClientHttps linkedDataRestClient;
    // @MockBean RegistrationServer registrationServer;
    // @MockBean RegistrationClient registrationClient;
    // @MockBean TrustManagerWrapperWithTrustService nodeTrustManagerTLS;
    // **************************************************************************
    // STRUCTURE OF THIS CLASS
    //
    // - TESTS
    // moved to
    // - WonMessageRoutesExternalInterceptedTest.java
    // here, messages to another atom (i.e., to 'external') are intercepted
    // and checked, but not processed further
    //
    // - WonMessageRoutesExternalRoutedTest.java
    // here, messages 'to external' are routed back into the node logic,
    // so we can test complete conversations.
    //
    // In this class:
    //
    // - TEST UTILS
    //
    // - SET UP & CLASS SET UP
    //
    // - SET UP UTILS
    //
    // ****************************************************************
    // ------ New to this class? Take 30 seconds to review these ------
    //
    // -------------------------- T I P S -----------------------------
    //
    // ****************************************************************
    //
    // This class is very useful when changing something about the messaging
    // system on the WoN node. If you do that, the following steps might help:
    //
    // # Verbose test output
    // Shows the messages from/to Owner/Node/Matcher in the logs.
    // Just set the loglevel for the tests to DEBUG. (in
    // src/test/resources/logback.xml)
    //
    //
    // # Activate camel tracing:
    // Logs every processor and the content of the camel exchange
    //
    // - enable tracing by setting camelContext.setTracing(true) in a
    // @Before method, here in this class. Just search for 'tracing'
    //
    // - make sure the logger is configured to show the trace messages. It depends
    // on the camel version, currently it's
    // org.apache.camel.processor.interceptor.Tracer, which needs to be set to INFO
    //
    //
    // # FailResponder:
    // Helps you see exceptions.
    //
    // - Set the loglevel for the FailResponder class to DEBUG so you see the
    // stacktrace of exceptions.
    //
    // -To see more clearly where the exception happens, you can remove the
    // '.handled(true)' directive in the Exception handler. Shows you a very helpful
    // log output.
    //
    // **************************************************************************
    //
    Processor messageToSendIntoBody = new MessageToSendIntoBodyProcessor();

    public WonMessageRoutesTest() {
        // TODO Auto-generated constructor stub
    }

    /****************************************
     * SETUP
     ****************************************/
    @BeforeClass
    public static void setUpBroker() throws Exception {
        brokerSvc = new BrokerService();
        brokerSvc.setBrokerName("TestBroker");
        brokerSvc.addConnector("tcp://localhost:61616");
        brokerSvc.setPersistenceAdapter(new MemoryPersistenceAdapter());
        brokerSvc.start();
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (executor != null) {
            executor.awaitTermination(1000, TimeUnit.MICROSECONDS);
            executor.shutdown();
        }
        if (brokerSvc != null) {
            brokerSvc.stop();
            brokerSvc.waitUntilStopped();
        }
    }

    /******************************************
     * TESTS
     ******************************************/
    protected WonMessage makeCreateAtomMessage(URI atomURI, String filename) throws IOException {
        Dataset atom1Content = loadDatasetAndReplaceAtomURI(filename, atomURI);
        WonMessage createAtom1Msg = WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content()
                        /**/.dataset(atom1Content)
                        .build();
        return createAtom1Msg;
    }

    /******************************************
     * TEST UTILS
     ******************************************/
    protected Predicate isFailureResponseTo(WonMessage msg) {
        Objects.requireNonNull(msg);
        return isFailureResponseTo(msg.getMessageURIRequired());
    }

    protected Predicate isFailureResponseTo(URI messageURI) {
        Objects.requireNonNull(messageURI);
        return (Exchange ex) -> {
            WonMessage msg = getMessageRequired(ex);
            boolean result = msg.getMessageType().isFailureResponse()
                            && messageURI.equals(msg.getRespondingToMessageURI());
            if (!result) {
                Optional<WonMessage> response = msg.getResponse();
                if (response.isPresent()) {
                    result = response.get().getMessageType().isFailureResponse()
                                    && messageURI.equals(response.get().getRespondingToMessageURI());
                }
                if (!result) {
                    response = msg.getRemoteResponse();
                    if (response.isPresent()) {
                        result = response.get().getMessageType().isFailureResponse()
                                        && messageURI.equals(response.get().getRespondingToMessageURI());
                    }
                }
            }
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "messageURI", messageURI, msg);
            }
            return result;
        };
    }

    protected Predicate isSuccessResponseTo(WonMessage msg) {
        Objects.requireNonNull(msg);
        return isSuccessResponseTo(msg.getMessageURIRequired());
    }

    protected Predicate isSuccessResponseTo(URI messageURI) {
        return new IsSuccessResponseTo(messageURI);
    }

    public Predicate isMessageContained(WonMessage message) {
        return isMessageContained(message.getMessageURIRequired());
    }

    public Predicate isMessageContained(URI messageURI) {
        return new IsMessageContained(messageURI);
    }

    protected Predicate or(Predicate... clauses) {
        return new Or(clauses);
    }

    protected Predicate and(Predicate... clauses) {
        return new And(clauses);
    }

    protected Predicate isMessageWithoutResponse(WonMessage msg) {
        Objects.requireNonNull(msg);
        return isMessageWithoutResponse(msg.getMessageURIRequired());
    }

    protected Predicate isMessageWithoutResponse(URI messageURI) {
        return new IsMessageWithoutResponse(messageURI);
    }

    protected Predicate isSocketHintFor(URI socketURI) {
        return new IsSocketHintFor(socketURI);
    }

    protected Predicate isAtomCreatedNotification(URI atomURI) {
        return new IsAtomCreatedNotification(atomURI);
    }

    protected Predicate isChangeNotificationFor(URI atomURI) {
        return new IsChangeNotificationFor(atomURI);
    }

    public WonMessage getMessageRequired(Exchange ex) {
        return getMessageFromBody(ex).orElseGet(() -> getMessageToSendRequired(ex));
    }

    public Optional<WonMessage> getMessage(Exchange ex) {
        return Optional.ofNullable(getMessageFromBody(ex).orElseGet(() -> getMessageToSend(ex).orElse(null)));
    }

    protected Predicate maxOnce(final Predicate pred) {
        Objects.requireNonNull(pred);
        return new MatchesMaxNTimesPredicate(1, pred);
    }

    protected Predicate maxTwice(final Predicate pred) {
        Objects.requireNonNull(pred);
        return new MatchesMaxNTimesPredicate(2, pred);
    }

    protected Predicate maxThreeTimes(final Predicate pred) {
        Objects.requireNonNull(pred);
        return new MatchesMaxNTimesPredicate(3, pred);
    }

    protected Predicate isMessageAndResponse(final URI msgUri) {
        return new IsMessageAndResponse(msgUri);
    }

    protected Predicate isMessageAndResponse(WonMessage msg) {
        return isMessageAndResponse(msg.getMessageURIRequired());
    }

    protected Predicate isResponseContainsSender() {
        return new ResponseContainsConnection();
    }

    protected Predicate isMessageAndResponseAndRemoteResponse(WonMessage msg) {
        return isMessageAndResponseAndRemoteResponse(msg.getMessageURIRequired());
    }

    protected Predicate isMessageAndResponseAndRemoteResponse(final URI msgUri) {
        return new IsMessageAndResponseAndRemoteResponse(msgUri);
    }

    protected Predicate isOwnResponseConfirmsNPrevious(int n) {
        return new OwnResponseConfirmsNPreviousMessages(n);
    }

    protected Predicate isRemoteResponseConfirmsNPrevious(int n) {
        return new RemoteResponseConfirmsNPreviousMessages(n);
    }

    protected Predicate isMessageConfirmsNPrevious(int n) {
        return new MessageConfirmsNPreviousMessages(n);
    }

    protected void assertConnectionAsExpected(Connection expected, Optional<Connection> con) {
        Assert.assertTrue("New connection should have been stored", con.isPresent());
        Assert.assertSame("Wrong state of connection", expected.getState(), con.get().getState());
        if (expected.getSocketURI() != null) {
            Assert.assertEquals("Wrong socket of connection", expected.getSocketURI(), con.get().getSocketURI());
        }
        if (expected.getTargetSocketURI() != null) {
            Assert.assertEquals("Wrong target socket of connection", expected.getTargetSocketURI(),
                            con.get().getTargetSocketURI());
        }
        if (expected.getAtomURI() != null) {
            Assert.assertEquals("Wrong atom uri of connection", expected.getAtomURI(),
                            con.get().getAtomURI());
        }
        if (expected.getTargetAtomURI() != null) {
            Assert.assertEquals("Wrong target atom uri of connection", expected.getTargetAtomURI(),
                            con.get().getTargetAtomURI());
        }
    }

    protected void prepareMockitoStubs(URI atomURI, URI socketURI, URI atomURI2, URI socketURI2) {
        Mockito.when(linkedDataSource.getDataForPublicResource(eq(atomURI)))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        Mockito.when(linkedDataSource.getDataForPublicResource(eq(atomURI2)))
                        .then(x -> linkedDataService.getAtomDataset(atomURI2, false, null,
                                        WonAclEvalContext.allowAll()));
        Mockito.when(socketLookup.getCapacity(eq(socketURI))).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.getCapacity(eq(socketURI2))).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.isCompatible(socketURI, socketURI2)).thenReturn(true);
        Mockito.when(socketLookup.isCompatible(socketURI2, socketURI)).thenReturn(true);
        Mockito.when(socketLookup.isCompatibleSocketTypes(any(URI.class), any(URI.class))).thenReturn(true);
        Mockito.when(socketLookup.getCapacityOfType(any(URI.class))).then(x -> Optional.of(10));
        Mockito.when(messageRoutingInfoServiceWithLookup.senderSocketType(any(WonMessage.class)))
                        .thenReturn(Optional.of(WXCHAT.ChatSocket.asURI()));
        Mockito.when(messageRoutingInfoServiceWithLookup.recipientSocketType(any(WonMessage.class)))
                        .thenReturn(Optional.of(WXCHAT.ChatSocket.asURI()));
        Mockito.when(messageRoutingInfoServiceWithLookup.senderNode(any(WonMessage.class)))
                        .thenReturn(Optional.of(URI_NODE_1));
        Mockito.when(messageRoutingInfoServiceWithLookup.recipientNode(any(WonMessage.class)))
                        .thenReturn(Optional.of(URI_NODE_1));
        Mockito.when(uriService.createAclGraphURIForAtomURI(any(URI.class))).thenAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                URI uri = (URI) invocation.getArguments()[0];
                                return URI.create(uri + "#acl");
                            }
                        });
        Mockito.when(uriService.createSysInfoGraphURIForAtomURI(any(URI.class))).thenAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                URI uri = (URI) invocation.getArguments()[0];
                                return URI.create(uri + "#sysinfo");
                            }
                        });
    }

    protected void executeInSeparateThreadAndWaitForResult(Runnable checks) throws InterruptedException {
        Thread t = new Thread(checks);
        t.start();
        t.join();
    }

    /**
     * Loads the Dataset.
     *
     * @param resourceName
     * @return
     * @throws IOException
     */
    protected Dataset loadDataset(String resourceName) throws IOException {
        return loadDataset(resourceName, null, null);
    }

    /**
     * Loads the Dataset, replacing all occurrences of <code>search</code> by
     * <code>replacement</code>.
     *
     * @param resourceName
     * @param search
     * @param replacement
     * @return
     * @throws IOException
     */
    protected Dataset loadDataset(String resourceName, String search, String replacement) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw, StandardCharsets.UTF_8.name());
        String content = null;
        if (search != null && replacement != null) {
            content = sw.toString().replaceAll(Pattern.quote(search), replacement);
        } else {
            content = sw.toString();
        }
        Dataset dataset = DatasetFactory.createGeneral();
        dataset.begin(ReadWrite.WRITE);
        RDFDataMgr.read(dataset, new ByteArrayInputStream(content.getBytes()), RDFFormat.TRIG.getLang());
        is.close();
        dataset.commit();
        return dataset;
    }

    protected Model createTestModel(String resourceName) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, is, RDFFormat.TTL.getLang());
        is.close();
        return model;
    }

    protected WonMessage prepareFromOwner(WonMessage msg) {
        // add public key of the newly created atom
        msg = atomKeyGeneratorAndAdder.process(msg);
        // add signature:
        return signatureAdder.signWithAtomKey(msg);
    }

    protected WonMessage prepareFromMatcher(WonMessage msg) throws Exception {
        // add signature:
        return WonMessageSignerVerifier.seal(msg);
    }

    protected WonMessage prepareFromExternalOwner(WonMessage msg) {
        return signatureAdder.signWithAtomKey(msg);
    }

    protected WonMessage prepareFromSystem(WonMessage msg) {
        return signatureAdder.signWithDefaultKey(msg);
    }

    protected void sendFromOwner(WonMessage msg, String ownerApplicationIdForResponse) {
        if (Objects.equals(WONMSG.MESSAGE_SELF, msg.getMessageURIRequired())) {
            throw new IllegalArgumentException("message is not prepared, cannot send : " + msg.toShortStringForDebug());
        }
        logMessageRdf(makeMessageBox(" message OWNER => NODE"), msg);
        Map<String, Object> headers = new HashMap<>();
        headers.put(WonCamelConstants.OWNER_APPLICATION_ID_HEADER, ownerApplicationIdForResponse);
        send(null, headers, RdfUtils.writeDatasetToString(msg.getCompleteDataset(),
                        WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE), "direct:fromOwnerMock");
    }

    protected void sendFromMatcher(WonMessage msg) {
        if (Objects.equals(WONMSG.MESSAGE_SELF, msg.getMessageURIRequired())) {
            throw new IllegalArgumentException("message is not prepared, cannot send : " + msg.toShortStringForDebug());
        }
        logMessageRdf(makeMessageBox("message MATCHER => NODE"), msg);
        send(null, null, RdfUtils.writeDatasetToString(msg.getCompleteDataset(),
                        WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE), "direct:fromMatcherMock");
    }

    protected void sendFromExternalOwner(WonMessage msg) {
        if (Objects.equals(WONMSG.MESSAGE_SELF, msg.getMessageURIRequired())) {
            throw new IllegalArgumentException("message is not prepared, cannot send : " + msg.toShortStringForDebug());
        }
        logMessageRdf(makeMessageBox("message EXTERNAL => NODE"), msg);
        send(null, null, RdfUtils.writeDatasetToString(msg.getCompleteDataset(),
                        WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE), "direct:fromNodeMock");
    }

    protected void sendFromExternalSystem(WonMessage msg) {
        if (Objects.equals(WONMSG.MESSAGE_SELF, msg.getMessageURIRequired())) {
            throw new IllegalArgumentException("message is not prepared, cannot send : " + msg.toShortStringForDebug());
        }
        logMessageRdf(makeMessageBox("message EXTERNAL => NODE"), msg);
        send(null, null, RdfUtils.writeDatasetToString(msg.getCompleteDataset(),
                        WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE), "direct:fromNodeMock");
    }

    protected void send(Map properties, Map<String, Object> headers, Object body, String endpoint) {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setPattern(ExchangePattern.InOnly);
        Endpoint ep = camelContext.getEndpoint(endpoint);
        if (properties != null) {
            if (properties.containsKey("methodName")) {
                exchange.setProperty("methodName", properties.get("methodName"));
            }
            if (properties.containsKey("protocol")) {
                exchange.setProperty("protocol", properties.get("protocol"));
            }
        }
        if (headers != null) {
            exchange.getIn().setHeaders(headers);
        }
        exchange.getIn().setBody(body);
        producerTemplate.send(ep, exchange);
    }

    protected void logMessageRdf(String logText, WonMessage msg) {
        if (logger.isDebugEnabled()) {
            logger.debug("\n" + logText + "\n" + RdfUtils.toString(Prefixer.setPrefixes(msg.getCompleteDataset()),
                            Lang_WON.TRIG_WON_CONVERSATION));
        }
    }

    protected URI newAtomURI() {
        return URI.create(URI_NODE_1 + "/atom/atom" + counter.incrementAndGet() + "-" + randomString.nextString(20));
    }

    protected URI newConnectionURI(URI atomUri) {
        return URI.create(atomUri.toString() + "/c/connection" + counter.incrementAndGet() + "-"
                        + randomString.nextString(20));
    }

    protected URI newMessageURI() {
        return URI.create("wm:/message" + counter.incrementAndGet() + "-" + randomString.nextString(20));
    }

    protected Dataset loadDatasetAndReplaceAtomURI(String file, URI atomURI) throws IOException {
        Dataset atomDataset = loadDataset(file);
        AtomModelWrapper amw = new AtomModelWrapper(atomDataset);
        amw.setAtomURI(atomURI);
        return atomDataset;
    }

    protected void assertMockEndpointsSatisfiedAndReset(MockEndpoint... endpoints) throws Exception {
        for (int i = 0; i < endpoints.length; i++) {
            endpoints[i].assertIsSatisfied();
            endpoints[i].reset();
        }
    }

    protected void assertMockEndpointsSatisfiedAndReset(MessageCollector collector, MockEndpoint... endpoints)
                    throws Exception {
        for (int i = 0; i < endpoints.length; i++) {
            endpoints[i].assertIsSatisfied();
            collector.collectFrom(endpoints[i].getExchanges());
            endpoints[i].reset();
        }
    }

    @Before
    public void configureMockEndpoint() throws Exception {
        camelContext.setTracing(true);
        ModelCamelContext context = (ModelCamelContext) camelContext;
        AdviceWith.adviceWith(
                        context.getRouteDefinition("activemq:queue:" +
                                        FROM_NODE_QUEUENAME),
                        context,
                        new AdviceWithRouteBuilder() {
                            @Override
                            public void configure() throws Exception {
                                replaceFromWith("direct:fromNodeMock");
                            }
                        });
        AdviceWith.adviceWith(
                        context.getRouteDefinition("activemq:queue:" + FROM_OWNER_QUEUENAME),
                        context,
                        new AdviceWithRouteBuilder() {
                            @Override
                            public void configure() throws Exception {
                                replaceFromWith("direct:fromOwnerMock");
                            }
                        });
        AdviceWith.adviceWith(
                        context.getRouteDefinition("activemq:queue:" + FROM_MATCHER_QUEUENAME), context,
                        new AdviceWithRouteBuilder() {
                            @Override
                            public void configure() throws Exception {
                                replaceFromWith("direct:fromMatcherMock");
                            }
                        });
        // for some reason, we have to add the advice to each route
        // don't try to do it by iterating over routes etc. Doesn't work.
        AdviceWith.adviceWith(
                        context.getRouteDefinition("direct:sendToOwner"),
                        context, adviceForOwnerProtocolOut());
        AdviceWith.adviceWith(
                        context.getRouteDefinition("direct:reactToMessage"),
                        context,
                        adviceForMatcherProtocolOut());
        //
        // MockEndpoint intercepting messages to owners
        toOwnerMockEndpoint.reset();
        toOwnerMockEndpoint.setResultWaitTime(5000);
        toOwnerMockEndpoint
                        .setReporter(exchange -> {
                            Optional<String> ownerAppId = WonCamelHelper.getOwnerApplicationId(exchange);
                            String ownerAppString = ownerAppId.isPresent() ? " [" + ownerAppId.get() + "]"
                                            : "";
                            logMessageRdf(
                                            makeMessageBox("message NODE => OWNER" + ownerAppString),
                                            WonMessage.of(RdfUtils.readDatasetFromString(
                                                            (String) exchange.getIn().getBody(),
                                                            WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE)));
                        });
        //
        // MockEndpoint intercepting messages to registered matchers
        // camelContext.addRoutes(new RouteBuilder() {
        // @Override
        // public void configure() throws Exception {
        // from("direct:toMatcherMock").to("mock:seda:MatcherProtocolOut");
        // }
        // });
        toMatcherMockEndpoint.reset();
        toMatcherMockEndpoint.setResultWaitTime(5000);
        toMatcherMockEndpoint
                        .setReporter(exchange -> {
                            try {
                                Optional<String> ownerAppId = WonCamelHelper.getOwnerApplicationId(exchange);
                                Optional<String> wonMessageString = Optional
                                                .ofNullable((String) exchange.getIn().getBody());
                                if (wonMessageString.isPresent() && wonMessageString.get().trim().length() > 0) {
                                    logMessageRdf(makeMessageBox("message NODE => MATCHER"),
                                                    WonMessage.of(RdfUtils.readDatasetFromString(wonMessageString.get(),
                                                                    WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE)));
                                } else {
                                    logger.debug(makeMessageBox("message NODE => MATCHER (no WonMessage)"));
                                }
                            } catch (WonMessageNotWellFormedException e) {
                                logger.info("swallowed WonMessageNotWellFormedException - this can happen");
                            }
                        });
        Mockito.when(wonNodeInformationService
                        .generateAtomURI(any(URI.class)))
                        .then(x -> newAtomURI());
        Mockito.when(wonNodeInformationService
                        .generateConnectionURI(any(URI.class)))
                        .then(invocation -> newConnectionURI((URI) invocation.getArguments()[0]));
        Mockito.when(wonNodeInformationService
                        .generateAtomURI())
                        .then(x -> newAtomURI());
        Mockito.when(wonNodeInformationService.getWonNodeInformation(URI_NODE_1)).then(x -> new WonNodeInfoBuilder()
                        .setAtomURIPrefix(URI_NODE_1 + "/atom")
                        .setWonNodeURI(URI_NODE_1.toString())
                        .setConnectionURIPrefix(URI_NODE_1 + "/connection")
                        .build());
        Mockito.when(uriService.isAtomURI(any(URI.class))).thenReturn(true);
        Mockito.when(uriService.getAtomResourceURIPrefix()).then(x -> URI_NODE_1.toString() + "/atom");
        Mockito.when(uriService.getResourceURIPrefix()).then(x -> URI_NODE_1.toString());
        Mockito.when(uriService.createSysInfoGraphURIForAtomURI(any(URI.class))).thenCallRealMethod();
        // Mockito.when(linkedDataSource.getDataForResource(any(URI.class),
        // any(URI.class)))
        // .thenThrow(new UnsupportedOperationException("cannot do linkeddata lookups
        // during tests"));
        // Mockito.when(linkedDataSource.getDataForResource(any(URI.class)))
        // .thenThrow(new UnsupportedOperationException("cannot do linkeddata lookups
        // during tests"));
        Mockito.when(linkedDataSource.getDataForPublicResource(eq(URI_NODE_1)))
                        .then(x -> linkedDataService.getNodeDataset());
        mockForMessageFromNode1ToNode1();
    }

    protected String makeMessageAsciiBox(String text) {
        int padding = 5;
        int width = text.length() + 2 * padding;
        StringBuilder box = new StringBuilder();
        box.append('\u2554');
        for (int i = 0; i < width; i++) {
            box.append('\u2550');
        }
        box.append('\u2557');
        box.append("\n\u2551");
        for (int i = 0; i < padding; i++) {
            box.append(' ');
        }
        box.append(text);
        for (int i = 0; i < padding; i++) {
            box.append(' ');
        }
        box.append("\u2551\n");
        box.append('\u255A');
        for (int i = 0; i < width; i++) {
            box.append('\u2550');
        }
        box.append('\u255D');
        return "\n" + box.toString() + "\n";
    }

    protected String makeMessageBox(String text) {
        int padding = 5;
        int width = text.length() + 2 * padding;
        StringBuilder box = new StringBuilder();
        box.append('+');
        for (int i = 0; i < width; i++) {
            box.append('=');
        }
        box.append('+');
        box.append("\n|");
        for (int i = 0; i < padding; i++) {
            box.append(' ');
        }
        box.append(text);
        for (int i = 0; i < padding; i++) {
            box.append(' ');
        }
        box.append("|\n");
        box.append('+');
        for (int i = 0; i < width; i++) {
            box.append('=');
        }
        box.append('+');
        return "\n" + box.toString() + "\n";
    }

    /****************************************
     * SETUP UTILS
     ****************************************/
    protected AdviceWithRouteBuilder adviceForOwnerProtocolOut() {
        return new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("bean:toOwnerSender")
                                .skipSendToOriginalEndpoint()
                                .bean(messageToSendIntoBody)
                                .to(toOwnerMockEndpoint);
            }
        };
    }

    protected AdviceWithRouteBuilder adviceForMatcherProtocolOut() {
        return new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("seda:MatcherProtocolOut")
                                .skipSendToOriginalEndpoint()
                                .to(toMatcherMockEndpoint);
            }
        };
    }

    protected void mockForMessageFromNode1ToNode1() {
        Mockito.when(messageRoutingInfoServiceWithLookup.senderNode(any(WonMessage.class)))
                        .thenReturn(Optional.of(URI_NODE_1));
        Mockito.when(messageRoutingInfoServiceWithLookup.recipientNode(any(WonMessage.class)))
                        .thenReturn(Optional.of(URI_NODE_1));
    }

    public void logMessageForFailedPredicate(String className, String parameterName, Object parameterValue,
                    WonMessage msg) {
        if (logger.isInfoEnabled()) {
            logger.info("predicate {} ({}: {}), does not match message {}:\n{}",
                            className, parameterName, parameterValue,
                            msg.getMessageURI(),
                            RdfUtils.toString(Prefixer.setPrefixes(msg.getCompleteDataset()),
                                            Lang_WON.TRIG_WON_CONVERSATION));
        }
    }

    private static class MatchesMaxNTimesPredicate implements Predicate {
        private final AtomicInteger matchesLeft;
        private final Predicate delegate;

        public MatchesMaxNTimesPredicate(int n, Predicate delegate) {
            Objects.requireNonNull(delegate);
            this.delegate = delegate;
            matchesLeft = new AtomicInteger(n);
        }

        @Override
        public boolean matches(Exchange exchange) {
            if (matchesLeft.get() <= 0) {
                return false;
            }
            boolean result = delegate.matches(exchange);
            if (result) {
                matchesLeft.decrementAndGet();
            }
            return result;
        }
    }

    private class IsFailureResponseTo implements Predicate {
        private final URI messageURI;

        public IsFailureResponseTo(URI messageURI) {
            Objects.requireNonNull(messageURI);
            this.messageURI = messageURI;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean result = msg.getMessageType().isFailureResponse()
                            && messageURI.equals(msg.getRespondingToMessageURI());
            if (!result) {
                Optional<WonMessage> response = msg.getResponse();
                if (response.isPresent()) {
                    result = response.get().getMessageType().isFailureResponse()
                                    && messageURI.equals(response.get().getRespondingToMessageURI());
                }
                if (!result) {
                    response = msg.getRemoteResponse();
                    if (response.isPresent()) {
                        result = response.get().getMessageType().isFailureResponse()
                                        && messageURI.equals(response.get().getRespondingToMessageURI());
                    }
                }
            }
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "messageURI", messageURI, msg);
            }
            return result;
        }
    }

    private class IsSuccessResponseTo implements Predicate {
        private final URI messageURI;

        public IsSuccessResponseTo(URI messageURI) {
            Objects.requireNonNull(messageURI);
            this.messageURI = messageURI;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean result = msg.getMessageType().isSuccessResponse()
                            && messageURI.equals(msg.getRespondingToMessageURI());
            if (!result) {
                Optional<WonMessage> response = msg.getResponse();
                if (response.isPresent()) {
                    result = response.get().getMessageType().isSuccessResponse()
                                    && messageURI.equals(response.get().getRespondingToMessageURI());
                }
                if (!result) {
                    response = msg.getRemoteResponse();
                    if (response.isPresent()) {
                        result = response.get().getMessageType().isSuccessResponse()
                                        && messageURI.equals(response.get().getRespondingToMessageURI());
                    }
                }
            }
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "messageURI", messageURI, msg);
            }
            return result;
        }
    }

    private class IsMessageContained implements Predicate {
        private final URI messageURI;

        public IsMessageContained(URI messageURI) {
            Objects.requireNonNull(messageURI);
            this.messageURI = messageURI;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean result = msg.getAllMessages().stream()
                            .anyMatch(m -> m.getMessageURIRequired().equals(this.messageURI));
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "messageURI", messageURI, msg);
            }
            return result;
        }
    }

    private class Or implements Predicate {
        private final Predicate[] clauses;

        public Or(Predicate... clauses) {
            Objects.requireNonNull(clauses);
            this.clauses = clauses;
        }

        @Override
        public boolean matches(Exchange exchange) {
            for (int i = 0; i < clauses.length; i++) {
                if (clauses[i].matches(exchange)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class And implements Predicate {
        private final Predicate[] clauses;

        public And(Predicate... clauses) {
            Objects.requireNonNull(clauses);
            this.clauses = clauses;
        }

        @Override
        public boolean matches(Exchange exchange) {
            for (int i = 0; i < clauses.length; i++) {
                if (!clauses[i].matches(exchange)) {
                    return false;
                }
            }
            return true;
        }
    }

    private class IsMessageWithoutResponse implements Predicate {
        private final URI messageURI;

        public IsMessageWithoutResponse(URI messageURI) {
            Objects.requireNonNull(messageURI);
            this.messageURI = messageURI;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean result = msg.getMessageURIRequired().equals(messageURI) && (!msg.getResponse().isPresent());
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "messageURI", messageURI, msg);
            }
            return result;
        }
    }

    private class IsSocketHintFor implements Predicate {
        private final URI socketURI;

        public IsSocketHintFor(URI socketURI) {
            Objects.requireNonNull(socketURI);
            this.socketURI = socketURI;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean result = ((msg.getMessageTypeRequired().isSocketHintMessage()
                            && socketURI.equals(msg.getRecipientSocketURIRequired())));
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "socketURI", socketURI, msg);
            }
            return result;
        }
    }

    private class IsAtomCreatedNotification implements Predicate {
        private final URI atomURI;

        public IsAtomCreatedNotification(URI atomURI) {
            Objects.requireNonNull(atomURI);
            this.atomURI = atomURI;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean result = (msg.getMessageTypeRequired().isAtomCreatedNotification()
                            && msg.getSenderAtomURIRequired().equals(atomURI));
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "atomURI", atomURI, msg);
            }
            return result;
        }
    }

    private class IsChangeNotificationFor implements Predicate {
        private final URI atomURI;

        public IsChangeNotificationFor(URI atomURI) {
            Objects.requireNonNull(atomURI);
            this.atomURI = atomURI;
        }

        public boolean matches(Exchange ex) {
            WonMessage msg = getMessageRequired(ex);
            boolean result = (msg.getMessageTypeRequired().isChangeNotification()
                            && msg.getSenderAtomURIRequired().equals(atomURI));
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "atomURI", atomURI, msg);
            }
            return result;
        }
    }

    private class IsMessageAndResponse implements Predicate {
        private final URI msgUri;

        public IsMessageAndResponse(URI msgUri) {
            Objects.requireNonNull(msgUri);
            this.msgUri = msgUri;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean ret = Objects.equals(msgUri, msg.getMessageURIRequired())
                            && msg.getResponse().isPresent()
                            && !msg.getRemoteResponse().isPresent();
            if (!ret) {
                logMessageForFailedPredicate(getClass().getName(), "msgUri", msgUri, msg);
            }
            return ret;
        }
    }

    private class ResponseContainsConnection implements Predicate {
        public ResponseContainsConnection() {
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            Optional<WonMessage> resp = msg.getResponse();
            if (resp.isPresent()) {
                if (resp.get().getConnectionURI() == null) {
                    logMessageForFailedPredicate(getClass().getName(), "[no parameter]", null, msg);
                    return false;
                }
            }
            resp = msg.getRemoteResponse();
            if (resp.isPresent()) {
                if (resp.get().getConnectionURI() == null) {
                    logMessageForFailedPredicate(getClass().getName(), "[no parameter]", null, msg);
                    return false;
                }
            }
            if (msg.isRemoteResponse()) {
                if (msg.getConnectionURI() == null) {
                    logMessageForFailedPredicate(getClass().getName(), "[no parameter]", null, msg);
                    return false;
                }
            }
            return true;
        }
    }

    private class IsMessageAndResponseAndRemoteResponse implements Predicate {
        private final URI msgUri;

        public IsMessageAndResponseAndRemoteResponse(URI msgUri) {
            Objects.requireNonNull(msgUri);
            this.msgUri = msgUri;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean result = Objects.equals(msgUri, msg.getMessageURIRequired())
                            && msg.getResponse().isPresent()
                            && msg.getRemoteResponse().isPresent();
            if (result) {
                Set<URI> s = new HashSet<>();
                s.add(msg.getMessageURI());
                s.add(msg.getResponse().get().getMessageURI());
                s.add(msg.getRemoteResponse().get().getMessageURI());
                result = s.size() == 3;
            }
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "msgUri", msgUri, msg);
            }
            return result;
        }
    }

    private class OwnResponseConfirmsNPreviousMessages implements Predicate {
        private final int n;

        public OwnResponseConfirmsNPreviousMessages(int n) {
            super();
            this.n = n;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            Optional<WonMessage> resp = msg.getResponse();
            boolean result = false;
            if (resp.isPresent()) {
                result = resp.get().getPreviousMessageURIs().size() == n;
            } else {
                result = n == 0;
            }
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "n", Integer.valueOf(n), msg);
            }
            return result;
        }
    }

    private class RemoteResponseConfirmsNPreviousMessages implements Predicate {
        private final int n;

        public RemoteResponseConfirmsNPreviousMessages(int n) {
            super();
            this.n = n;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            Optional<WonMessage> resp = msg.getRemoteResponse();
            boolean result = false;
            if (resp.isPresent()) {
                result = resp.get().getPreviousMessageURIs().size() == n;
            } else {
                result = n == 0;
            }
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "n", Integer.valueOf(n), msg);
            }
            return result;
        }
    }

    private class MessageConfirmsNPreviousMessages implements Predicate {
        private final int n;

        public MessageConfirmsNPreviousMessages(int n) {
            super();
            this.n = n;
        }

        @Override
        public boolean matches(Exchange exchange) {
            WonMessage msg = getMessageRequired(exchange);
            boolean result = false;
            result = msg.getPreviousMessageURIs().size() == n;
            if (!result) {
                logMessageForFailedPredicate(getClass().getName(), "n", Integer.valueOf(n), msg);
            }
            return result;
        }
    }

    private class MessageToSendIntoBodyProcessor implements Processor {
        public MessageToSendIntoBodyProcessor() {
        }

        public void process(Exchange exchange) throws Exception {
            exchange.getIn().setBody(WonMessageEncoder.encode(getMessageToSendRequired(exchange),
                            WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE));
        }
    }

    protected class MessageCollector {
        Dataset collected = DatasetFactory.createGeneral();

        public void collectFrom(Collection<Exchange> exchanges) {
            exchanges.forEach(this::collectFrom);
        }

        public void collectFrom(Exchange exchange) {
            Optional<WonMessage> msg = getMessage(exchange);
            if (msg.isPresent()) {
                RdfUtils.addDatasetToDataset(collected, msg.get().getCompleteDataset());
            }
        }

        public Dataset getCollected() {
            return RdfUtils.cloneDataset(collected);
        }
    }
}
