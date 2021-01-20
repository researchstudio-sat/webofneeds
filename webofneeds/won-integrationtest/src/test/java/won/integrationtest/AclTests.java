package won.integrationtest;

import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.model.AseRoot;
import won.auth.model.AtomExpression;
import won.auth.model.Authorization;
import won.auth.model.Individuals;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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
                try {
                    // make sure we can read the atom using our webid
                    Dataset data = ctx.getLinkedDataSource().getDataForResource(atomUri, atomUri);
                    if (data == null || data.isEmpty()) {
                        failTest(bus, "WoN node sent null or empty response for atom data with owner's WebID");
                        return;
                    }
                } catch (Exception e) {
                    failTest(bus, "Exception when requesting atom data with owner's WebID", e);
                    return;
                }
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                try {
                    // make sure we cannot get the atom's content without the webid
                    Dataset data = ctx.getLinkedDataSource().getDataForResource(atomUri, null);
                    if (data == null || data.isEmpty()) {
                        failTest(bus, "WoN node sent null or empty response for atom data without owner's WebID");
                        return;
                    }
                } catch (Exception e) {
                    failTest(bus, "Exception when requesting atom data without owner's WebID", e);
                    return;
                }
                passTest(bus);
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testCreateAtomWithoutACL_isPubliclyReadable() throws Exception {
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
                try {
                    // make sure we can read the atom using our webid
                    Dataset data = ctx.getLinkedDataSource().getDataForResource(atomUri, atomUri);
                    if (data == null || data.isEmpty()) {
                        failTest(bus, "WoN node sent null or empty response for atom data with owner's WebID");
                        return;
                    }
                } catch (Exception e) {
                    failTest(bus, "Exception when requesting atom data with owner's WebID", e);
                    return;
                }
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                try {
                    // make sure we cannot get the atom's content without the webid
                    Dataset data = ctx.getLinkedDataSource().getDataForResource(atomUri, null);
                    if (data == null || data.isEmpty()) {
                        failTest(bus, "WoN node sent null or empty response for atom data without owner's WebID");
                        return;
                    }
                } catch (Exception e) {
                    failTest(bus, "Exception when requesting atom data without owner's WebID", e);
                    return;
                }
                passTest(bus);
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
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                try {
                    // make sure we can read the atom using our webid
                    Dataset data = ctx.getLinkedDataSource().getDataForResource(atomUri, atomUri);
                    if (data == null || data.isEmpty()) {
                        failTest(bus, "WoN node sent null or empty response for atom data with owner's WebID");
                        return;
                    }
                } catch (Exception e) {
                    failTest(bus, "Exception when requesting atom data with owner's WebID", e);
                    return;
                }
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                try {
                    // make sure we cannot get the atom's content without the webid
                    Dataset data = ctx.getLinkedDataSource().getDataForResource(atomUri, null);
                    failTest(bus, "Expected exception not thrown when trying to access atom data without atom's WebID");
                    return;
                } catch (Exception e) {
                    // we expect an exception because we are not allowed to see the atom's content
                    // without a WebID
                    logger.debug("caught Exception {}:{}", e.getClass().getSimpleName(), e.getMessage());
                }
                passTest(bus);
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
                    ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                    try {
                        // make sure we can read the atom using our webid
                        Dataset data = ctx.getLinkedDataSource().getDataForResource(atomUri1, atomUri2);
                        if (data == null || data.isEmpty()) {
                            failTest(bus, "WoN node sent null or empty response for atom1 data with WebID of atom2");
                            return;
                        }
                    } catch (Exception e) {
                        failTest(bus, "Exception when requesting atom1 data with WebID of atom2", e);
                        return;
                    }
                    ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                    try {
                        // make sure we cannot get the atom's content without the webid
                        Dataset data = ctx.getLinkedDataSource().getDataForResource(atomUri2, atomUri1);
                        failTest(bus,
                                        "Expected exception not thrown when trying to access for atom2 data with WebID of atom1");
                        return;
                    } catch (Exception e) {
                        // we expect an exception because we are not allowed to see the atom's content
                        // without a WebID
                        logger.debug("caught Exception {}:{}", e.getClass().getSimpleName(), e.getMessage());
                    }
                    passTest(bus);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            logger.debug("Finished initializing test 'testQueryBasedMatch()'");
        });
    }
}
