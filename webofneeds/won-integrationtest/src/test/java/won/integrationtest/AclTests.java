package won.integrationtest;

import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.model.*;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.behaviour.BehaviourBarrier;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WXBUDDY;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXHOLD;
import won.utils.content.model.AtomContent;
import won.utils.content.model.RdfOutput;
import won.utils.content.model.Socket;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AclTests extends AbstractBotBasedTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test(timeout = 60 * 1000)
    public void testCreateAtomWithEmptyACL_isPubliclyReadable() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = new AtomContent(atomUriString);
            atomContent.addTitle("Unit Test Atom ACL Test 1");
            final Socket holderSocket = new Socket(atomUriString + "#holderSocket");
            holderSocket.setSocketDefinition(WXHOLD.HolderSocket.asURI());
            final Socket buddySocket = new Socket(atomUriString + "#buddySocket");
            buddySocket.setSocketDefinition(WXBUDDY.BuddySocket.asURI());
            atomContent.addSocket(holderSocket);
            atomContent.addSocket(buddySocket);
            atomContent.addType(URI.create(WON.Atom.getURI()));
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content()
                            /**/.graph(RdfOutput.toGraph(atomContent))
                            .content()
                            /**/.aclGraph(GraphFactory.createGraphMem()) // add an empty acl graph
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                boolean passed = true;
                passed = passed && testLinkedDataRequestOk(ctx, bus, "withWebid", atomUri, atomUri);
                passed = passed && testLinkedDataRequestOkNoWebId(ctx, bus, "withoutWebId", atomUri);
                if (passed) {
                    passTest(bus);
                }
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testAtomWithoutACL_fallbackToLegacyImpl() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = new AtomContent(atomUriString);
            atomContent.addTitle("Unit Test Atom ACL Test 2");
            final Socket holderSocket = new Socket(atomUriString + "#holderSocket");
            holderSocket.setSocketDefinition(WXHOLD.HolderSocket.asURI());
            final Socket buddySocket = new Socket(atomUriString + "#buddySocket");
            buddySocket.setSocketDefinition(WXBUDDY.BuddySocket.asURI());
            atomContent.addSocket(holderSocket);
            atomContent.addSocket(buddySocket);
            atomContent.addType(URI.create(WON.Atom.getURI()));
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content()
                            /**/.graph(RdfOutput.toGraph(atomContent))
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                URI connContainerUri = uriService.createConnectionContainerURIForAtom(atomUri);
                URI createMessageUri = ((SuccessResponseEvent) event).getOriginalMessageURI();
                boolean passed = true;
                passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri, atomUri, createMessageUri);
                passed = passed && testLinkedDataRequestOk_emptyDataset(ctx, bus, "test2.", atomUri, connContainerUri);
                passed = passed && testLinkedDataRequestOkNoWebId(ctx, bus, "test3.", atomUri);
                passed = passed && testLinkedDataRequestOkNoWebId_emptyDataset(ctx, bus, "test4.", connContainerUri);
                passed = passed && testLinkedDataRequestFailsNoWebId(ctx, bus, "test5.",
                                LinkedDataFetchingException.class,
                                createMessageUri);
                if (passed) {
                    passTest(bus);
                }
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testCreateAtomWithACL_isNotPubliclyReadable() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = new AtomContent(atomUriString);
            atomContent.addTitle("Unit Test Atom ACL Test 3 (with acl)");
            final Socket holderSocket = new Socket(atomUriString + "#holderSocket");
            holderSocket.setSocketDefinition(WXHOLD.HolderSocket.asURI());
            final Socket buddySocket = new Socket(atomUriString + "#buddySocket");
            buddySocket.setSocketDefinition(WXBUDDY.BuddySocket.asURI());
            atomContent.addSocket(holderSocket);
            atomContent.addSocket(buddySocket);
            atomContent.addType(URI.create(WON.Atom.getURI()));
            // create an acl allowing only the atom itself to read everything
            Authorization auth = new Authorization();
            AtomExpression ae = new AtomExpression();
            ae.addAtomsURI(URI.create("http://example.com/nobody"));
            auth.addGranteesAtomExpression(ae);
            AseRoot g = new AseRoot();
            g.addOperationsSimpleOperationExpression(Individuals.OP_READ);
            auth.addGrant(g);
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content()
                            /**/.graph(RdfOutput.toGraph(atomContent))
                            .content()
                            /**/.aclGraph(won.auth.model.RdfOutput.toGraph(auth)) // add the acl graph
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                URI connContainerUri = uriService.createConnectionContainerURIForAtom(atomUri);
                URI createMessageUri = ((SuccessResponseEvent) event).getOriginalMessageURI();
                boolean passed = true;
                passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri, atomUri, createMessageUri);
                passed = passed && testLinkedDataRequestOk_emptyDataset(ctx, bus, "test2.", atomUri, connContainerUri);
                passed = passed && testLinkedDataRequestFailsNoWebId(ctx, bus, "test3.",
                                LinkedDataFetchingException.class,
                                atomUri, connContainerUri, createMessageUri);
                if (passed) {
                    passTest(bus);
                }
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testExplicitReadAuthorization() throws Exception {
        final AtomicBoolean atom1Created = new AtomicBoolean(false);
        final AtomicBoolean atom2Created = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            {
                final String atomUriString = atomUri1.toString();
                final AtomContent atomContent = new AtomContent(atomUriString);
                atomContent.addTitle("Granting atom");
                final Socket chatSocket = new Socket(atomUriString + "#chatSocket");
                chatSocket.setSocketDefinition(WXCHAT.ChatSocket.asURI());
                atomContent.addSocket(chatSocket);
                atomContent.addType(URI.create(WON.Atom.getURI()));
                Authorization auth = new Authorization();
                AtomExpression ae = new AtomExpression();
                ae.addAtomsURI(atomUri2);
                auth.addGranteesAtomExpression(ae);
                AseRoot g = new AseRoot();
                g.addOperationsSimpleOperationExpression(Individuals.OP_READ);
                auth.addGrant(g);
                WonMessage createMessage = WonMessageBuilder.createAtom()
                                .atom(atomUri1)
                                .content().graph(RdfOutput.toGraph(atomContent))
                                .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                .build();
                createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                final String action = "Create granting atom";
                EventListener successCallback = event -> {
                    logger.debug("Granting atom created");
                    createMessageUri1.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                    latch.countDown();
                };
                EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                action);
                EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                failureCallback, ctx);
                ctx.getWonMessageSender().sendMessage(createMessage);
            }
            // create match source
            {
                final String atomUriString = atomUri2.toString();
                final AtomContent atomContent = new AtomContent(atomUriString);
                atomContent.addTitle("Grantee atom");
                atomContent.addSparqlQuery(
                                "PREFIX won:<https://w3id.org/won/core#>\n"
                                                + "PREFIX con:<https://w3id.org/won/content#>\n"
                                                + "SELECT ?result (1.0 AS ?score) WHERE {"
                                                + "?result a won:Atom ;"
                                                + "    con:tag \"tag-to-match\"."
                                                + "}");
                final Socket chatSocket = new Socket(atomUriString + "#chatSocket");
                chatSocket.setSocketDefinition(WXCHAT.ChatSocket.asURI());
                atomContent.addSocket(chatSocket);
                atomContent.addType(URI.create(WON.Atom.getURI()));
                Authorization auth = new Authorization();
                AtomExpression ae = new AtomExpression();
                ae.addAtomsURI(URI.create("http://example.com/nobody"));
                auth.addGranteesAtomExpression(ae);
                AseRoot g = new AseRoot();
                g.addOperationsSimpleOperationExpression(Individuals.OP_READ);
                auth.addGrant(g);
                WonMessage createMessage = WonMessageBuilder.createAtom()
                                .atom(atomUri2)
                                .content().graph(RdfOutput.toGraph(atomContent))
                                .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                .build();
                createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                final String action = "Create grantee atom";
                EventListener successCallback = event -> {
                    logger.debug("Grantee atom created");
                    createMessageUri2.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                    latch.countDown();
                };
                EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                action);
                EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                failureCallback, ctx);
                ctx.getWonMessageSender().sendMessage(createMessage);
            }
            ctx.getExecutor().execute(() -> {
                try {
                    latch.await();
                    URI connContainerUri1 = uriService.createConnectionContainerURIForAtom(atomUri1);
                    URI connContainerUri2 = uriService.createConnectionContainerURIForAtom(atomUri2);
                    boolean passed = true;
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri1, atomUri1,
                                    createMessageUri1.get());
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test2.", atomUri2, atomUri1,
                                    createMessageUri1.get());
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test3.", atomUri1,
                                    LinkedDataFetchingException.class,
                                    atomUri2, connContainerUri2, createMessageUri2.get());
                    passed = passed && testLinkedDataRequestOk_emptyDataset(ctx, bus, "test5.", atomUri1,
                                    connContainerUri1);
                    passed = passed && testLinkedDataRequestOk_emptyDataset(ctx, bus, "test6.", atomUri2,
                                    connContainerUri1, connContainerUri2);
                    passed = passed && testLinkedDataRequestFailsNoWebId(ctx, bus, "test7.",
                                    LinkedDataFetchingException.class,
                                    atomUri1, connContainerUri1, atomUri2, connContainerUri2, createMessageUri1.get(),
                                    createMessageUri2.get());
                    if (passed) {
                        passTest(bus);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            logger.debug("Finished initializing test 'testQueryBasedMatch()'");
        });
    }

    @Test(timeout = 600 * 1000)
    public void testConnectionMessages() throws Exception {
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        final AtomicReference<URI> connectionUri12 = new AtomicReference<>();
        final AtomicReference<URI> connectionUri21 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri12 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri21 = new AtomicReference<>();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final BotBehaviour bbCreateAtom1 = new BotBehaviour(ctx, "bbCreateAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    final AtomContent atomContent = new AtomContent(atomUriString);
                    atomContent.addTitle("Connection initiating atom");
                    final Socket chatSocket = new Socket(atomUriString + "#chatSocket");
                    chatSocket.setSocketDefinition(WXCHAT.ChatSocket.asURI());
                    atomContent.addSocket(chatSocket);
                    atomContent.addType(URI.create(WON.Atom.getURI()));
                    Authorization auth = new Authorization();
                    AtomExpression ae = new AtomExpression();
                    ae.addAtomsURI(atomUri2);
                    auth.addGranteesAtomExpression(ae);
                    AseRoot r = new AseRoot();
                    GraphExpression gr = new GraphExpression();
                    gr.addOperationsSimpleOperationExpression(Individuals.OP_READ);
                    r.addGraph(gr);
                    SocketExpression s = new SocketExpression();
                    s.setInherit(false);
                    r.addSocket(s);
                    auth.addGrant(r);
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    EventListener successCallback = event -> {
                        logger.debug("Connection initiating atom created");
                        createMessageUri1.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection initiating atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            // create match source
            final BotBehaviour bbCreateAtom2 = new BotBehaviour(ctx, "bbCreateAtom2") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    final AtomContent atomContent = new AtomContent(atomUriString);
                    atomContent.addTitle("Grantee atom");
                    atomContent.addSparqlQuery(
                                    "PREFIX won:<https://w3id.org/won/core#>\n"
                                                    + "PREFIX con:<https://w3id.org/won/content#>\n"
                                                    + "SELECT ?result (1.0 AS ?score) WHERE {"
                                                    + "?result a won:Atom ;"
                                                    + "    con:tag \"tag-to-match\"."
                                                    + "}");
                    final Socket chatSocket = new Socket(atomUriString + "#chatSocket");
                    chatSocket.setSocketDefinition(WXCHAT.ChatSocket.asURI());
                    atomContent.addSocket(chatSocket);
                    atomContent.addType(URI.create(WON.Atom.getURI()));
                    Authorization auth = new Authorization();
                    AtomExpression ae = new AtomExpression();
                    ae.addAtomsURI(atomUri1);
                    auth.addGranteesAtomExpression(ae);
                    AseRoot r = new AseRoot();
                    GraphExpression gr = new GraphExpression();
                    gr.addOperationsSimpleOperationExpression(Individuals.OP_READ);
                    r.addGraph(gr);
                    SocketExpression s = new SocketExpression();
                    s.setInherit(false);
                    r.addSocket(s);
                    auth.addGrant(r);
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepting atom created");
                        createMessageUri2.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection accepting atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbSendConnect = new BotBehaviour(ctx, "bbSendConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#chatSocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#chatSocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello there!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri12.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection requested");
                        connectionUri12.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect = new BotBehaviour(ctx, "bbAcceptConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#chatSocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#chatSocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri21.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepted");
                        connectionUri21.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Accepting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbSendMessage = new BotBehaviour(ctx, "bbSendMessage") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage msg = WonMessageBuilder.connectionMessage()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#chatSocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#chatSocket"))
                                    .direction().fromOwner()
                                    .content().text("Nice, this works")
                                    .build();
                    msg = ctx.getWonMessageSender().prepareMessage(msg);
                    final URI connectMessageUri = msg.getMessageURIRequired();
                    EventListener successCallback = event -> {
                        logger.debug("Message sent");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Sending connection message");
                    EventBotActionUtils.makeAndSubscribeResponseListener(msg, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(msg);
                }
            };
            BotBehaviour bbTestLinkedDataAccess = new BotBehaviour(ctx, "bbTestLinkedDataAccess") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    URI connContainerUri1 = uriService.createConnectionContainerURIForAtom(atomUri1);
                    URI connContainerUri2 = uriService.createConnectionContainerURIForAtom(atomUri2);
                    boolean passed = true;
                    // both atoms allow each other read access
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri1,
                                    atomUri1,
                                    atomUri2,
                                    // createMessageUri1.get(),
                                    connectionUri12.get(),
                                    connectMessageUri12.get(),
                                    connectMessageUri21.get(),
                                    connContainerUri1);
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test2.", atomUri1,
                                    LinkedDataFetchingException.class,
                                    // createMessageUri2.get(),
                                    connectionUri21.get(),
                                    connContainerUri2);
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test3.", atomUri2,
                                    atomUri2,
                                    atomUri1,
                                    // createMessageUri2.get(),
                                    connectionUri21.get(),
                                    connectMessageUri21.get(),
                                    connectMessageUri12.get(),
                                    connContainerUri2);
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test4.", atomUri2,
                                    LinkedDataFetchingException.class,
                                    // createMessageUri1.get(),
                                    connectionUri12.get(),
                                    connContainerUri1);
                    if (passed) {
                        passTest(bus);
                    }
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbCreateAtom1, bbCreateAtom2);
            barrier.thenStart(bbSendConnect);
            barrier.activate();
            bbSendConnect.onDeactivateActivate(bbAcceptConnect);
            bbAcceptConnect.onDeactivateActivate(bbSendMessage);
            bbSendMessage.onDeactivateActivate(bbTestLinkedDataAccess);
            bbCreateAtom1.activate();
            bbCreateAtom2.activate();
        });
    }
}
