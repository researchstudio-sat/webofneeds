package won.integrationtest;

import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.linkeddata.AuthEnabledLinkedDataSource;
import won.auth.model.Authorization;
import won.auth.model.ConnectionState;
import won.auth.model.TargetAtomExpression;
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
import won.protocol.vocabulary.*;
import won.utils.content.model.AtomContent;
import won.utils.content.model.RdfOutput;
import won.utils.content.model.Socket;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static won.auth.model.Individuals.OP_READ;

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
            final AtomContent atomContent = AtomContent.builder(atomUri)
                            .addTitle("Unit Test Atom ACL Test 2")
                            .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                            .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                            .build())
                            .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                            .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                            .build())
                            .addType(URI.create(WON.Atom.getURI()))
                            .build();
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
            final AtomContent atomContent = AtomContent.builder(atomUri)
                            .addTitle("Unit Test Atom ACL Test 3 (with acl)")
                            .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                            .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                            .build())
                            .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                            .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                            .build())
                            .addType(URI.create(WON.Atom.getURI()))
                            .build();
            // create an acl allowing only the atom itself to read everything
            Authorization auth = Authorization.builder()
                            .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                            .addGranteesAtomExpression(ae -> ae.addAtomsURI(URI.create("https://example.com/nobody")))
                            .build();
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content().graph(RdfOutput.toGraph(atomContent))
                            .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth)) // add the acl graph
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
    public void testImplicitOwnerToken() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = AtomContent.builder(atomUri)
                            .addTitle("Unit test for implicit owner token")
                            .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                            .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                            .build())
                            .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                            .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                            .build())
                            .addType(URI.create(WON.Atom.getURI()))
                            .build();
            // create an acl allowing only the atom itself to read everything
            Authorization auth = Authorization.builder()
                            .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                            .addGranteesAtomExpression(ae -> ae.addAtomsURI(URI.create("https://example.com/nobody")))
                            .build();
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content().graph(RdfOutput.toGraph(atomContent))
                            .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth)) // add the acl graph
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                URI connContainerUri = uriService.createConnectionContainerURIForAtom(atomUri);
                URI createMessageUri = ((SuccessResponseEvent) event).getOriginalMessageURI();
                boolean passed = true;
                URI tokenQuery = uriService
                                .createTokenRequestURIForAtomURIWithScopes(atomUri, WONAUTH.OwnerToken.toString());
                passed = testTokenRequest(ctx, bus, null, false,
                                atomUri, null, tokenQuery, "test1.1 - obtain token");
                Set<String> tokens = ((AuthEnabledLinkedDataSource) ctx.getLinkedDataSource())
                                .getAuthTokens(tokenQuery, atomUri);
                String token = tokens.stream().findFirst().get();
                passed = passed && testTokenRequest(ctx, bus, null, true,
                                null, token, tokenQuery, "test1.2 - request token using only token (empty result)");
                passed = passed && testLinkedDataRequest(ctx, bus, LinkedDataFetchingException.class, true,
                                null, null, atomUri, "test1.3 - request atom data without any auth (fails)");
                passed = passed && testLinkedDataRequest(ctx, bus, null, true,
                                atomUri, null, atomUri, "test1.4 - request atom data with webid (success");
                passed = passed && testLinkedDataRequest(ctx, bus, null, true,
                                null, token, atomUri, "test1.5 - request atom data with token (success");
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
                final AtomContent atomContent = AtomContent.builder(atomUriString)
                                .addTitle("Granting atom")
                                .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                .build())
                                .addType(URI.create(WON.Atom.getURI()))
                                .build();
                Authorization auth = Authorization.builder()
                                .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri2))
                                .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                                .build();
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
                final AtomContent atomContent = AtomContent.builder(atomUriString)
                                .addTitle("Grantee atom")
                                .addSparqlQuery(
                                                "PREFIX won:<https://w3id.org/won/core#>\n"
                                                                + "PREFIX con:<https://w3id.org/won/content#>\n"
                                                                + "SELECT ?result (1.0 AS ?score) WHERE {"
                                                                + "?result a won:Atom ;"
                                                                + "    con:tag \"tag-to-match\"."
                                                                + "}")
                                .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                .build())
                                .addType(URI.create(WON.Atom.getURI()))
                                .build();
                Authorization auth = Authorization.builder()
                                .addGranteesAtomExpression(
                                                ae -> ae.addAtomsURI(URI.create("http://example.com/nobody")))
                                .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                                .build();
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

    @Test(timeout = 60 * 1000)
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
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Connection initiating atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri2))
                                    .addGrant(ase -> ase
                                                    .addGraph(ge -> ge.addOperationsSimpleOperationExpression(OP_READ)))
                                    .build();
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
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Grantee atom")
                                    .addSparqlQuery(
                                                    "PREFIX won:<https://w3id.org/won/core#>\n"
                                                                    + "PREFIX con:<https://w3id.org/won/content#>\n"
                                                                    + "SELECT ?result (1.0 AS ?score) WHERE {"
                                                                    + "?result a won:Atom ;"
                                                                    + "    con:tag \"tag-to-match\"."
                                                                    + "}")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri1))
                                    .addGrant(ase -> ase
                                                    .addGraph(ge -> ge.addOperationsSimpleOperationExpression(OP_READ)))
                                    .build();
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

    @Test(timeout = 600 * 1000)
    public void testTokenExchange() throws Exception {
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        final AtomicReference<URI> createMessageUri3 = new AtomicReference();
        final AtomicReference<URI> connectionUri12 = new AtomicReference<>();
        final AtomicReference<URI> connectionUri21 = new AtomicReference<>();
        final AtomicReference<URI> connectionUri23 = new AtomicReference<>();
        final AtomicReference<URI> connectionUri32 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri12 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri21 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri23 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri32 = new AtomicReference<>();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri3 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final BotBehaviour bbCreateAtom1 = new BotBehaviour(ctx, "bbCreateAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri1)
                                    .addTitle("atom 1/3")
                                    .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth1 = getBuddiesAndBOfBuddiesReadAllGraphsAuth();
                    Authorization auth2 = getBuddiesReceiveBuddyTokenAuth();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth1))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth2))
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
            final BotBehaviour bbCreateAtom2 = new BotBehaviour(ctx, "bbCreateAtom2") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri2)
                                    .addTitle("atom 2/3")
                                    .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth1 = getBuddiesAndBOfBuddiesReadAllGraphsAuth();
                    Authorization auth2 = getBuddiesReceiveBuddyTokenAuth();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth1))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth2))
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
            final BotBehaviour bbCreateAtom3 = new BotBehaviour(ctx, "bbCreateAtom3") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri3.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri3)
                                    .addTitle("atom 3/3")
                                    .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth1 = getBuddiesAndBOfBuddiesReadAllGraphsAuth();
                    Authorization auth2 = getBuddiesReceiveBuddyTokenAuth();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri3)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth1))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth2))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri3);
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepting atom created");
                        createMessageUri3.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection accepting atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbSendConnect12 = new BotBehaviour(ctx, "bbSendConnect12") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#buddySocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#buddySocket"))
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
            BotBehaviour bbAcceptConnect21 = new BotBehaviour(ctx, "bbAcceptConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#buddySocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#buddySocket"))
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
            BotBehaviour bbSendConnect32 = new BotBehaviour(ctx, "bbSendConnect32") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri3.toString() + "#buddySocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#buddySocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello there!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri32.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection requested");
                        connectionUri32.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect23 = new BotBehaviour(ctx, "bbAcceptConnect23") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#buddySocket"))
                                    .recipient(URI.create(atomUri3.toString() + "#buddySocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri23.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepted");
                        connectionUri23.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Accepting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbTestLinkedDataAccess = new BotBehaviour(ctx, "bbTestLinkedDataAccess") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    URI connContainerUri1 = uriService.createConnectionContainerURIForAtom(atomUri1);
                    URI connContainerUri2 = uriService.createConnectionContainerURIForAtom(atomUri2);
                    URI connContainerUri3 = uriService.createConnectionContainerURIForAtom(atomUri3);
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
                                    connContainerUri2,
                                    atomUri3,
                                    connContainerUri3);
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test3.", atomUri2,
                                    atomUri2,
                                    atomUri1,
                                    // createMessageUri2.get(),
                                    connectionUri21.get(),
                                    connectMessageUri21.get(),
                                    connectMessageUri12.get(),
                                    connContainerUri2,
                                    atomUri3);
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test4.", atomUri2,
                                    LinkedDataFetchingException.class,
                                    // createMessageUri1.get(),
                                    connectionUri12.get(),
                                    connContainerUri1,
                                    connContainerUri3,
                                    connectionUri32.get());
                    deactivate();
                }
            };
            BotBehaviour bbRequestBuddyToken = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    testLinkedDataRequest(ctx, bus, LinkedDataFetchingException.class, false, null, null, atomUri3,
                                    "test 5.1 - read atom3");
                    URI tokenrequest = uriService
                                    .createTokenRequestURIForAtomURIWithScopes(atomUri2,
                                                    WXBUDDY.BuddySocket.asString());
                    testTokenRequest(ctx, bus, IllegalArgumentException.class, false, null, null, tokenrequest,
                                    "test5.2 - request token");
                    Set<String> resultingTokens = new HashSet();
                    testTokenRequest(ctx, bus, null, false, atomUri1, null, tokenrequest,
                                    "test5.3 - request buddy socket token from atom2 using webID atom1",
                                    resultingTokens);
                    String token = resultingTokens.stream().findFirst().get();
                    testLinkedDataRequest(ctx, bus, null, false, null, token, atomUri3,
                                    "test 5.4- read atom3 with token issued for atom 1");
                    testLinkedDataRequest(ctx, bus, null, false, null, token, atomUri2,
                                    "test 5.5 - read atom2 with token issued for atom 1");
                    deactivate();
                }
            };
            BotBehaviour bbTestLinkedDataAccessWithToken = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    passTest(bus);
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbCreateAtom1, bbCreateAtom2, bbCreateAtom3);
            barrier.thenStart(bbSendConnect12);
            barrier.activate();
            bbSendConnect12.onDeactivateActivate(bbAcceptConnect21);
            bbAcceptConnect21.onDeactivateActivate(bbSendConnect32);
            bbSendConnect32.onDeactivateActivate(bbAcceptConnect23);
            bbAcceptConnect23.onDeactivateActivate(bbTestLinkedDataAccess);
            bbTestLinkedDataAccess.onDeactivateActivate(bbRequestBuddyToken);
            bbRequestBuddyToken.onDeactivateActivate(bbTestLinkedDataAccessWithToken);
            bbCreateAtom1.activate();
            bbCreateAtom2.activate();
            bbCreateAtom3.activate();
        });
    }

    private Authorization getBuddiesReceiveBuddyTokenAuth() {
        // buddies and buddies of buddies can see my content
        Authorization auth = Authorization.builder()
                        .addGranteesAseRoot(ase -> ase
                                        .addSocket(se -> se
                                                        .addSocketType(WXBUDDY.BuddySocket.asURI())
                                                        .addConnection(ce -> ce
                                                                        .addConnectionState(
                                                                                        ConnectionState.CONNECTED)
                                                                        .setTargetAtom(new TargetAtomExpression()))))
                        .addGrant(ase -> ase
                                        .addOperationsTokenOperationExpression(top -> top
                                                        .setRequestToken(rt -> rt
                                                                        .setTokenScopeURI(
                                                                                        WXBUDDY.BuddySocket.asURI()))))
                        .build();
        return auth;
    }

    private Authorization getBuddiesAndBOfBuddiesReadAllGraphsAuth() {
        // buddies and buddies of buddies can see my content
        Authorization auth = Authorization.builder()
                        .addGranteesAseRoot(ase -> ase
                                        .addSocket(se -> se
                                                        .addSocketType(WXBUDDY.BuddySocket.asURI())
                                                        .addConnection(ce -> ce
                                                                        .addConnectionState(ConnectionState.CONNECTED)
                                                                        .setTargetAtom(new TargetAtomExpression()))))
                        .addBearer(ts -> ts
                                        .setNodeSigned(true)
                                        .addTokenScopesURI(WXBUDDY.BuddySocket.asURI())
                                        .addIssuersAseRoot(i -> i
                                                        .addSocket(se -> se
                                                                        .addSocketType(WXBUDDY.BuddySocket.asURI())
                                                                        .addConnection(ce -> ce
                                                                                        .addConnectionState(
                                                                                                        ConnectionState.CONNECTED)
                                                                                        .setTargetAtom(new TargetAtomExpression())))))
                        .addGrant(ase -> ase
                                        .addGraph(ge -> ge.addOperationsSimpleOperationExpression(OP_READ)))
                        .build();
        return auth;
    }
}
