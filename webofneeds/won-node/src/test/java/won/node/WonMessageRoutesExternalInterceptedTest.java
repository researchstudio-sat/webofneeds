package won.node;

import org.apache.activemq.broker.BrokerService;
import org.apache.camel.EndpointInject;
import org.apache.camel.Predicate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.jena.query.Dataset;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import won.node.springsecurity.acl.WonAclEvalContext;
import won.node.test.WonMessageRoutesTestHelper;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.*;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;

public class WonMessageRoutesExternalInterceptedTest extends WonMessageRoutesTest {
    static BrokerService brokerSvc;
    @EndpointInject(uri = "mock:direct:AtomProtocolOut")
    protected MockEndpoint toNodeMockEndpoint;
    @Autowired
    WonMessageRoutesTestHelper helper;

    /*******************************************
     * Create tests
     *******************************************/
    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_create_atom__error_multiple_atoms() throws Exception {
        URI atomURI = newAtomURI(); // uri in the file
        Dataset atom1Content = loadDataset(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_multiple_atoms.ttl",
                        "http://example.com/atom1", atomURI.toString());
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(createAtom1Msg.getMessageURI()));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        Assert.assertFalse("No atom should have been created",
                        atomService.getAtom(atomURI).isPresent());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_create_atom__error_no_atom() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDataset(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_no_atom.ttl", "http://example.com/atom1",
                        atomURI.toString());
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(createAtom1Msg.getMessageURI()));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        Assert.assertFalse("No atom should have been created",
                        atomService.getAtom(atomURI).isPresent());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_create_atom__error_no_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_no_socket.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(createAtom1Msg.getMessageURI()));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        Assert.assertFalse("No atom should have been created",
                        atomService.getAtom(atomURI).isPresent());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_create_atom__error_wrong_default_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_wrong_default_socket.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isSuccessResponseTo(createAtom1Msg.getMessageURI()));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_create_atom__error_wrong_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_wrong_socket.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(createAtom1Msg.getMessageURI()));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        Assert.assertFalse("No atom should have been created",
                        atomService.getAtom(atomURI).isPresent());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_create_atom__error_missing_socket_definition() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_missing_socket_definition.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(createAtom1Msg.getMessageURI()));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        Assert.assertFalse("No atom should have been created",
                        atomService.getAtom(atomURI).isPresent());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_create_atom__error_contains_subpath() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_contains_subpath.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(createAtom1Msg.getMessageURI()));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        Assert.assertFalse("No atom should have been created",
                        atomService.getAtom(atomURI).isPresent());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_create_atom() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
        Assert.assertTrue("An atom should have been created", atomService.getAtom(atomURI).isPresent());
        Assert.assertTrue("A message should have been stored",
                        messageService.getMessage(createAtom1Msg.getMessageURI(), atomURI).isPresent());
        Assert.assertTrue("A #socket1 should have been stored", socketRepository
                        .findOneBySocketURI(URI.create(atomURI.toString() + "#socket1")).isPresent());
        Assert.assertTrue("A #socket2 should have been stored", socketRepository
                        .findOneBySocketURI(URI.create(atomURI.toString() + "#socket2")).isPresent());
    }

    /*******************************************
     * Replace tests
     *******************************************/
    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__successful() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__replacement.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(replaceMsg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__error_contains_subpath() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_contains_subpath.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(replaceMsg.getMessageURI()));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__error_missing_socket_definition() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_missing_socket_definition.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(replaceMsg.getMessageURI()));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__error_multiple_atoms() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toNodeMockEndpoint.expectedMessageCount(0);
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDataset(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_multiple_atoms.ttl",
                        "http://example.com/atom1", atomURI.toString());
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(replaceMsg.getMessageURI()));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        toNodeMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__error_no_atom() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDataset(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_no_atom.ttl", "http://example.com/atom1",
                        atomURI.toString());
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(replaceMsg.getMessageURI()));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__error_no_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_no_socket.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(replaceMsg.getMessageURI()));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__error_wrong_default_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_wrong_default_socket.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isSuccessResponseTo(replaceMsg.getMessageURI()));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__error_wrong_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__error_wrong_socket.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isFailureResponseTo(replaceMsg.getMessageURI()));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__successful_add_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__replacement_add_socket.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(replaceMsg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__successful_change_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__replacement_change_socket.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(replaceMsg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_replace_atom__successful_remove_socket() throws Exception {
        URI atomURI = newAtomURI();
        Dataset atom1Content = loadDatasetAndReplaceAtomURI("/won/node/WonMessageRoutesTest/data/test-atom1.ttl",
                        atomURI);
        WonMessage createAtom1Msg = prepareFromOwner(WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atom1Content)
                        .build());
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        // new expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        Dataset atom1ReplaceContent = loadDatasetAndReplaceAtomURI(
                        "/won/node/WonMessageRoutesTest/data/test-atom1__replacement_remove_socket.ttl",
                        atomURI);
        WonMessage replaceMsg = prepareFromOwner(WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atom1ReplaceContent)
                        .build());
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        toOwnerMockEndpoint.expectedMessageCount(2);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(replaceMsg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        sendFromOwner(replaceMsg, OWNERAPPLICATION_ID_OWNER1);
        toMatcherMockEndpoint.assertIsSatisfied();
    }

    /*******************************************
     * SocketHint tests
     *******************************************/
    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_socketHint__to_inactive() throws Exception {
        URI atomURI = newAtomURI();
        toMatcherMockEndpoint.expectedMessageCount(1); // notifications for create & deactivate
        toOwnerMockEndpoint.expectedMessageCount(1); // echoes and responses for create & deactivate
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // create
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        WonMessage deactivateMsg = prepareFromOwner(WonMessageBuilder
                        .deactivate()
                        .atom(atomURI)
                        .direction().fromOwner()
                        .build());
        // deactivate
        assertMockEndpointsSatisfiedAndReset(toMatcherMockEndpoint, toOwnerMockEndpoint);
        toMatcherMockEndpoint.expectedMessageCount(1); // notifications for create & deactivate
        toOwnerMockEndpoint.expectedMessageCount(1); // echoes and responses for create & deactivate
        sendFromOwner(deactivateMsg, OWNERAPPLICATION_ID_OWNER1);
        List<Socket> sockets = socketRepository.findByAtomURI(atomURI);
        URI socketURI = sockets.get(0).getSocketURI();
        URI targetSocketURI = URI.create("uri:some-other-atom#socket");
        Mockito.when(socketLookup.isCompatible(socketURI, targetSocketURI)).then(x -> true);
        // set new expectations for hint
        toMatcherMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.assertIsSatisfied();
        toOwnerMockEndpoint.reset();
        toMatcherMockEndpoint.reset();
        toMatcherMockEndpoint.expectedMessageCount(0);
        toOwnerMockEndpoint.expectedMessageCount(0);
        WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                        .socketHint()
                        .recipientSocket(socketURI)
                        .hintTargetSocket(targetSocketURI)
                        .hintScore(0.5)
                        .build());
        sendFromMatcher(socketHintMessage);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_socketHint__error_incompatible_sockets() throws Exception {
        URI atomURI = newAtomURI();
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
        Atom atom = atomService.getAtom(atomURI).get();
        List<Socket> sockets = socketRepository.findByAtomURI(atomURI);
        URI socketURI = sockets.get(0).getSocketURI();
        URI targetSocketURI = URI.create("uri:some-other-atom#socket");
        Mockito.when(socketLookup.isCompatible(socketURI, targetSocketURI)).then(x -> false);
        Mockito.when(socketLookup.getSocketType(any())).then(x -> Optional.empty());
        // set new expectations for hint
        toOwnerMockEndpoint.reset();
        toMatcherMockEndpoint.reset();
        toMatcherMockEndpoint.expectedMessageCount(0);
        toOwnerMockEndpoint.expectedMessageCount(0);
        WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                        .socketHint()
                        .recipientSocket(socketURI)
                        .hintTargetSocket(targetSocketURI)
                        .hintScore(0.5)
                        .build());
        sendFromMatcher(socketHintMessage);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
        Assert.assertTrue("At least one socket should have been stored", sockets.size() > 0);
        Assert.assertTrue("An atom should have been created",
                        atomService.getAtom(atomURI).isPresent());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_socketHint__error_no_such_socket() throws Exception {
        URI atomURI = newAtomURI();
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
        Assert.assertTrue("An atom should have been created",
                        atomService.getAtom(atomURI).isPresent());
        Atom atom = atomService.getAtom(atomURI).get();
        List<Socket> sockets = socketRepository.findByAtomURI(atomURI);
        URI socketURI = URI.create(
                        atom.getAtomURI().toString() + "#socket-dontfind-" + Math.floor(Math.random() * 1000000));
        Set<URI> socketURIs = sockets.stream().map(Socket::getSocketURI).collect(Collectors.toSet());
        while (socketURIs.contains(socketURI)) {
            socketURI = URI.create(
                            atom.getAtomURI().toString() + "#socket-dontfind-" + Math.floor(Math.random() * 1000000));
        }
        URI targetSocketURI = URI.create("uri:some-other-atom#socket");
        Mockito.when(socketLookup.isCompatible(socketURI, targetSocketURI)).then(x -> true);
        Assert.assertTrue("At least one socket should have been stored", sockets.size() > 0);
        // set new expectations for hint
        toOwnerMockEndpoint.reset();
        toMatcherMockEndpoint.reset();
        toMatcherMockEndpoint.expectedMessageCount(0);
        toOwnerMockEndpoint.expectedMessageCount(0);
        WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                        .socketHint()
                        .recipientSocket(socketURI)
                        .hintTargetSocket(targetSocketURI)
                        .hintScore(0.5)
                        .build());
        sendFromMatcher(socketHintMessage);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_socketHint__error_no_such_recipient_atom() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket");
        URI targetSocketURI = URI.create("uri:some-other-atom#socket");
        // set new expectations for hint
        toOwnerMockEndpoint.reset();
        toMatcherMockEndpoint.reset();
        toMatcherMockEndpoint.expectedMessageCount(0);
        toOwnerMockEndpoint.expectedMessageCount(0);
        WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                        .socketHint()
                        .recipientSocket(socketURI)
                        .hintTargetSocket(targetSocketURI)
                        .hintScore(0.5)
                        .build());
        sendFromMatcher(socketHintMessage);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_socketHint__success() throws Exception {
        URI atomURI = newAtomURI();
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
        Atom atom = atomService.getAtom(atomURI).get();
        List<Socket> sockets = socketRepository.findByAtomURI(atomURI);
        URI socketURI = sockets.get(0).getSocketURI();
        URI targetSocketURI = URI.create("uri:some-other-atom#socket");
        Assert.assertTrue("An atom should have been created", atomService.getAtom(atomURI).isPresent());
        Assert.assertTrue("At least one socket should have been stored", sockets.size() > 0);
        // set new expectations for hint
        Mockito.when(socketLookup.isCompatible(socketURI, targetSocketURI)).then(x -> true);
        Mockito.when(socketLookup.isCompatibleSocketTypes(any(), any())).then(x -> true);
        Mockito.when(socketLookup.getSocketType(targetSocketURI))
                        .thenReturn(Optional.of(URI.create("just:some-uri")));
        toOwnerMockEndpoint.reset();
        toMatcherMockEndpoint.reset();
        toMatcherMockEndpoint.expectedMessageCount(0);
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isSocketHintFor(socketURI));
        WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                        .socketHint()
                        .recipientSocket(socketURI)
                        .hintTargetSocket(targetSocketURI)
                        .hintScore(0.5)
                        .build());
        sendFromMatcher(socketHintMessage);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
    }

    /*******************************************
     * Deactivate tests
     *******************************************/
    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_deactivate__successful() throws Exception {
        URI atomURI = newAtomURI();
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
        // set new expectations for deactivate
        toOwnerMockEndpoint.reset();
        toMatcherMockEndpoint.reset();
        WonMessage deactivateMsg = prepareFromOwner(WonMessageBuilder
                        .deactivate()
                        .atom(atomURI)
                        .direction().fromOwner()
                        .build());
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(deactivateMsg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        sendFromOwner(deactivateMsg, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.assertIsSatisfied();
        toMatcherMockEndpoint.assertIsSatisfied();
        Assert.assertSame("Atom should have been deactivated",
                        AtomState.INACTIVE, atomService.getAtom(atomURI).get().getState());
        Assert.assertTrue("A message should have been stored",
                        messageService.getMessage(createAtom1Msg.getMessageURI(), atomURI).isPresent());
        Assert.assertTrue("A message should have been stored",
                        messageService.getMessage(deactivateMsg.getMessageURI(), atomURI).isPresent());
    }

    /*******************************************
     * Activate tests
     *******************************************/
    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_deactivate_reactivate_successful() throws Exception {
        URI atomURI = newAtomURI();
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(createAtom1Msg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage deactivateMsg = prepareFromOwner(WonMessageBuilder
                        .deactivate()
                        .atom(atomURI)
                        .direction().fromOwner()
                        .build());
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(deactivateMsg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        Mockito.when(linkedDataSource.getDataForResource(atomURI))
                        .then(x -> linkedDataService.getAtomDataset(atomURI, false, null,
                                        WonAclEvalContext.allowAll()));
        sendFromOwner(deactivateMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage reactivateMsg = prepareFromOwner(WonMessageBuilder
                        .activate()
                        .atom(atomURI)
                        .build());
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(reactivateMsg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        sendFromOwner(reactivateMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Assert.assertSame("Atom should have been reactivated",
                        AtomState.ACTIVE, atomService.getAtom(atomURI).get().getState());
        Assert.assertTrue("A message should have been stored",
                        messageService.getMessage(createAtom1Msg.getMessageURI(), atomURI).isPresent());
        Assert.assertTrue("A message should have been stored",
                        messageService.getMessage(deactivateMsg.getMessageURI(), atomURI).isPresent());
        Assert.assertTrue("A message should have been stored",
                        messageService.getMessage(reactivateMsg.getMessageURI(), atomURI).isPresent());
    }

    /*******************************************
     * Connect tests
     *******************************************/
    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        WonMessage createAtom2Msg = prepareFromOwner(makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(2);
        toOwnerMockEndpoint.expectedMessagesMatches(
                        or(isMessageAndResponse(createAtom1Msg),
                                        isMessageAndResponse(createAtom2Msg)));
        toMatcherMockEndpoint.expectedMessageCount(2);
        toMatcherMockEndpoint.expectedMessagesMatches(
                        or(isAtomCreatedNotification(atomURI), isAtomCreatedNotification(atomURI2)));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage connectMsg = prepareFromOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(socketURI).recipient(socketURI2)
                        .content().text("Unittest connect")
                        .build());
        // expectations for connect
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(connectMsg));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(connectMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.REQUEST_SENT);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, socketURI2);
        assertConnectionAsExpected(expected, con);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect__with_external_success_response() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        WonMessage createAtom2Msg = prepareFromOwner(makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(2);
        toOwnerMockEndpoint.expectedMessagesMatches(
                        or(isMessageAndResponse(createAtom1Msg),
                                        isMessageAndResponse(createAtom2Msg)));
        toMatcherMockEndpoint.expectedMessageCount(2);
        toMatcherMockEndpoint.expectedMessagesMatches(
                        or(isAtomCreatedNotification(atomURI), isAtomCreatedNotification(atomURI2)));
        toNodeMockEndpoint.setExpectedMessageCount(0);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        WonMessage connectMsg = prepareFromOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(socketURI).recipient(socketURI2)
                        .content().text("Unittest connect")
                        .build());
        // expectations for connect
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(connectMsg));
        toMatcherMockEndpoint.expectedMessageCount(0);
        toNodeMockEndpoint.expectedMessageCount(1);
        toNodeMockEndpoint.expectedMessagesMatches(isMessageAndResponse(connectMsg));
        sendFromOwner(connectMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        URI remoteCon = newConnectionURI(atomURI2);
        WonMessage successResponse = prepareFromSystem(WonMessageBuilder
                        .response()
                        .respondingToMessageFromExternal(connectMsg)
                        .fromConnection(remoteCon)
                        .success()
                        .build());
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageWithoutResponse(successResponse));
        toNodeMockEndpoint.expectedMessageCount(0);
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromExternalSystem(successResponse);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.REQUEST_SENT);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
        expected.setTargetConnectionURI(remoteCon);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, socketURI2);
        assertConnectionAsExpected(expected, con);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect__after_hint() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        WonMessage createAtom2Msg = prepareFromOwner(makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toOwnerMockEndpoint.expectedMessageCount(2);
        toMatcherMockEndpoint.expectedMessageCount(2);
        toNodeMockEndpoint.expectedMessageCount(0);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
        // set new expectations for hint
        Mockito.when(socketLookup.isCompatible(socketURI, socketURI2)).then(x -> true);
        WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                        .socketHint()
                        .recipientSocket(socketURI)
                        .hintTargetSocket(socketURI2)
                        .hintScore(0.5)
                        .build());
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        toOwnerMockEndpoint.expectedMessageCount(1);
        toMatcherMockEndpoint.expectedMessageCount(0);
        toNodeMockEndpoint.expectedMessageCount(0);
        sendFromMatcher(socketHintMessage);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        WonMessage connectMsg = prepareFromOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(socketURI).recipient(socketURI2)
                        .content().text("Unittest connect")
                        .build());
        // expectations for connect
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(connectMsg));
        toMatcherMockEndpoint.expectedMessageCount(0);
        toNodeMockEndpoint.expectedMessageCount(1);
        toNodeMockEndpoint.expectedMessagesMatches(isMessageAndResponse(connectMsg));
        sendFromOwner(connectMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        Connection expectedCon = new Connection();
        expectedCon.setState(ConnectionState.REQUEST_SENT);
        expectedCon.setSocketURI(socketURI);
        expectedCon.setTargetSocketURI(socketURI2);
        Optional<Connection> actualCon = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI,
                        socketURI2);
        assertConnectionAsExpected(expectedCon, actualCon);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect_from_external() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        WonMessage createAtom2Msg = prepareFromOwner(makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toOwnerMockEndpoint.expectedMessageCount(2);
        toOwnerMockEndpoint.expectedMessagesMatches(
                        or(isMessageAndResponse(createAtom1Msg), isMessageAndResponse(createAtom2Msg)));
        toMatcherMockEndpoint.expectedMessageCount(2);
        toNodeMockEndpoint.expectedMessageCount(0);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
        // set new expectations for connect
        WonMessage connectMsg = prepareFromOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(socketURI).recipient(socketURI2)
                        .content().text("Unittest connect")
                        .build());
        WonMessage response = prepareFromSystem(WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(connectMsg)
                        .fromConnection(newConnectionURI(atomURI))
                        .success()
                        .build());
        WonMessage msg = WonMessage.of(connectMsg, response);
        // expectations for connect
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponseAndRemoteResponse(msg));
        toMatcherMockEndpoint.expectedMessageCount(0);
        toNodeMockEndpoint.expectedMessageCount(1);
        toNodeMockEndpoint.expectedMessagesMatches(isSuccessResponseTo(msg));
        sendFromExternalOwner(msg);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.REQUEST_RECEIVED);
        expected.setSocketURI(socketURI2);
        expected.setTargetSocketURI(socketURI);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI2, socketURI);
        assertConnectionAsExpected(expected, con);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect_from_external__after_hint() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        WonMessage createAtom2Msg = prepareFromOwner(makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toOwnerMockEndpoint.expectedMessageCount(2);
        toMatcherMockEndpoint.expectedMessageCount(2);
        toNodeMockEndpoint.expectedMessageCount(0);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        // set new expectations for hint
        toOwnerMockEndpoint.expectedMessageCount(1);
        toMatcherMockEndpoint.expectedMessageCount(0);
        toNodeMockEndpoint.expectedMessageCount(0);
        WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                        .socketHint()
                        .recipientSocket(socketURI)
                        .hintTargetSocket(socketURI2)
                        .hintScore(0.5)
                        .build());
        sendFromMatcher(socketHintMessage);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        // set new expectations for connect
        WonMessage connectMsg = prepareFromOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(socketURI2).recipient(socketURI)
                        .content().text("Unittest connect")
                        .build());
        WonMessage response = prepareFromSystem(WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(connectMsg)
                        .fromConnection(newConnectionURI(atomURI))
                        .success()
                        .build());
        WonMessage msg = WonMessage.of(connectMsg, response);
        // expectations for connect
        toOwnerMockEndpoint.expectedMessageCount(1);
        toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponseAndRemoteResponse(connectMsg));
        toMatcherMockEndpoint.expectedMessageCount(0);
        toNodeMockEndpoint.expectedMessageCount(1);
        toNodeMockEndpoint.expectedMessagesMatches(isSuccessResponseTo(connectMsg));
        Assert.assertTrue("A #socket1 should have been stored", socketRepository
                        .findOneBySocketURI(URI.create(atomURI.toString() + "#socket1")).isPresent());
        Assert.assertTrue("A #socket2 should have been stored", socketRepository
                        .findOneBySocketURI(URI.create(atomURI.toString() + "#socket2")).isPresent());
        sendFromExternalOwner(msg);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.REQUEST_RECEIVED);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, socketURI2);
        assertConnectionAsExpected(expected, con);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect__after_connect_from_external__after_hint() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        WonMessage createAtom2Msg = prepareFromOwner(makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toOwnerMockEndpoint.expectedMessageCount(2);
        toMatcherMockEndpoint.expectedMessageCount(2);
        toNodeMockEndpoint.expectedMessageCount(0);
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
        WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                        .socketHint()
                        .recipientSocket(socketURI)
                        .hintTargetSocket(socketURI2)
                        .hintScore(0.5)
                        .build());
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        sendFromMatcher(socketHintMessage);
        toOwnerMockEndpoint.expectedMessageCount(1);
        toMatcherMockEndpoint.expectedMessageCount(0);
        toNodeMockEndpoint.expectedMessageCount(0);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        WonMessage connectFromExternalMsg = prepareFromExternalOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(socketURI2).recipient(socketURI)
                        .content().text("Unittest connect")
                        .build());
        WonMessage response = prepareFromSystem(WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(connectFromExternalMsg)
                        .fromConnection(newConnectionURI(atomURI))
                        .success()
                        .build());
        WonMessage msg = WonMessage.of(connectFromExternalMsg, response);
        // expectations for connect
        Assert.assertTrue("A #socket1 should have been stored", socketRepository
                        .findOneBySocketURI(URI.create(atomURI.toString() + "#socket1")).isPresent());
        Assert.assertTrue("A #socket2 should have been stored", socketRepository
                        .findOneBySocketURI(URI.create(atomURI.toString() + "#socket2")).isPresent());
        toOwnerMockEndpoint.expectedMessageCount(1);
        toMatcherMockEndpoint.expectedMessageCount(0);
        toNodeMockEndpoint.expectedMessageCount(1);
        sendFromExternalOwner(msg);
        WonMessage connectFromOwnerMsg = prepareFromOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(socketURI).recipient(socketURI2)
                        .content().text("Unittest connect")
                        .build());
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        toOwnerMockEndpoint.expectedMessageCount(1);
        // toOwnerMockEndpoint.expectedMessagesMatches(isMessageAndResponse(connectFromOwnerMsg));
        toMatcherMockEndpoint.expectedMessageCount(1);
        toNodeMockEndpoint.expectedMessageCount(1);
        // toNodeMockEndpoint.expectedMessagesMatches(isMessageAndResponse(connectFromOwnerMsg));
        sendFromOwner(connectFromOwnerMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.CONNECTED);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, socketURI2);
        assertConnectionAsExpected(expected, con);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test__hint__connect_from_external__connect__try_force_racecondition() throws Exception {
        for (int i = 0; i < 5; i++) {
            URI atomURI = newAtomURI();
            URI socketURI = URI.create(atomURI.toString() + "#socket1");
            URI atomURI2 = newAtomURI();
            URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
            prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
            WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                            "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
            WonMessage createAtom2Msg = prepareFromOwner(makeCreateAtomMessage(atomURI2,
                            "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
            // set minimal expectations just so we can expect something and subsequently
            // reset expectations
            toOwnerMockEndpoint.expectedMessageCount(2);
            toMatcherMockEndpoint.expectedMessageCount(2);
            toNodeMockEndpoint.expectedMessageCount(0);
            sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
            sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER2);
            assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
            toOwnerMockEndpoint.expectedMessageCount(1);
            toMatcherMockEndpoint.expectedMessageCount(0);
            toNodeMockEndpoint.expectedMessageCount(0);
            WonMessage socketHintMessage = prepareFromMatcher(WonMessageBuilder
                            .socketHint()
                            .recipientSocket(socketURI)
                            .hintTargetSocket(socketURI2)
                            .hintScore(0.5)
                            .build());
            sendFromMatcher(socketHintMessage);
            assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
            WonMessage connectFromExternalMsg = prepareFromExternalOwner(WonMessageBuilder
                            .connect()
                            .sockets().sender(socketURI2).recipient(socketURI)
                            .content().text("Unittest connect")
                            .build());
            WonMessage response = prepareFromSystem(WonMessageBuilder
                            .response()
                            .respondingToMessageFromOwner(connectFromExternalMsg)
                            .fromConnection(newConnectionURI(atomURI))
                            .success()
                            .build());
            WonMessage msg = WonMessage.of(connectFromExternalMsg, response);
            WonMessage connectFromOwnerMsg = prepareFromOwner(WonMessageBuilder
                            .connect()
                            .sockets().sender(socketURI).recipient(socketURI2)
                            .content().text("Unittest connect")
                            .build());
            toOwnerMockEndpoint.expectedMessageCount(2);
            Predicate pred1 = maxOnce(isMessageAndResponseAndRemoteResponse(msg));
            Predicate pred2 = maxOnce(isMessageAndResponse(connectFromOwnerMsg));
            toOwnerMockEndpoint.expectedMessagesMatches(
                            or(pred1, pred2),
                            or(pred1, pred2));
            toMatcherMockEndpoint.expectedMessageCount(2);
            toNodeMockEndpoint.expectedMessageCount(2);
            pred1 = maxOnce(isSuccessResponseTo(msg));
            pred2 = maxOnce(isMessageAndResponse(connectFromOwnerMsg));
            toNodeMockEndpoint.expectedMessagesMatches(
                            or(pred1, pred2),
                            or(pred1, pred2));
            Thread t1 = new Thread(() -> helper.doInSeparateTransaction(
                            () -> sendFromExternalOwner(msg)));
            Thread t2 = new Thread(() -> helper.doInSeparateTransaction(
                            () -> sendFromOwner(connectFromOwnerMsg, OWNERAPPLICATION_ID_OWNER1)));
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
            Connection expected = new Connection();
            expected.setState(ConnectionState.CONNECTED);
            expected.setSocketURI(socketURI);
            expected.setTargetSocketURI(socketURI2);
            Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, socketURI2);
            assertConnectionAsExpected(expected, con);
        }
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test__connect_from_external__connect__try_force_racecondition() throws Exception {
        for (int i = 0; i < 20; i++) {
            toOwnerMockEndpoint.reset();
            toMatcherMockEndpoint.reset();
            toNodeMockEndpoint.reset();
            URI atomURI = newAtomURI();
            URI socketURI = URI.create(atomURI.toString() + "#socket1");
            URI atomURI2 = newAtomURI();
            URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
            prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
            WonMessage createAtom1Msg = prepareFromOwner(makeCreateAtomMessage(atomURI,
                            "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
            WonMessage createAtom2Msg = prepareFromOwner(makeCreateAtomMessage(atomURI2,
                            "/won/node/WonMessageRoutesTest/data/test-atom1.ttl"));
            // set minimal expectations just so we can expect something and subsequently
            // reset expectations
            toOwnerMockEndpoint.expectedMessageCount(2);
            toMatcherMockEndpoint.expectedMessageCount(2);
            toNodeMockEndpoint.expectedMessageCount(0);
            sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
            sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
            assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
            WonMessage connectFromExternalMsg = prepareFromExternalOwner(WonMessageBuilder
                            .connect()
                            .sockets().sender(socketURI2).recipient(socketURI)
                            .content().text("Unittest connect")
                            .build());
            WonMessage response = prepareFromSystem(WonMessageBuilder
                            .response()
                            .respondingToMessageFromOwner(connectFromExternalMsg)
                            .fromConnection(newConnectionURI(atomURI))
                            .success()
                            .build());
            WonMessage msg = WonMessage.of(connectFromExternalMsg, response);
            WonMessage connectFromOwnerMsg = prepareFromOwner(WonMessageBuilder
                            .connect()
                            .sockets().sender(socketURI).recipient(socketURI2)
                            .content().text("Unittest connect")
                            .build());
            toOwnerMockEndpoint.expectedMessageCount(2);
            toOwnerMockEndpoint.expectedMessagesMatches(
                            or(isMessageAndResponseAndRemoteResponse(msg), isMessageAndResponse(connectFromOwnerMsg)));
            toMatcherMockEndpoint.expectedMessageCount(1);
            toNodeMockEndpoint.expectedMessageCount(2);
            toNodeMockEndpoint.expectedMessagesMatches(
                            or(isMessageAndResponse(connectFromOwnerMsg),
                                            isSuccessResponseTo(msg)));
            Thread t1 = new Thread(() -> helper.doInSeparateTransaction(
                            () -> sendFromExternalOwner(msg)));
            Thread t2 = new Thread(() -> helper.doInSeparateTransaction(
                            () -> sendFromOwner(connectFromOwnerMsg, OWNERAPPLICATION_ID_OWNER1)));
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint, toNodeMockEndpoint);
            Connection expected = new Connection();
            expected.setState(ConnectionState.CONNECTED);
            expected.setSocketURI(socketURI);
            expected.setTargetSocketURI(socketURI2);
            Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, socketURI2);
            assertConnectionAsExpected(expected, con);
        }
    }

    @Before
    public void setUp() throws Exception {
        toNodeMockEndpoint.reset();
        toNodeMockEndpoint.setResultWaitTime(5000);
        toNodeMockEndpoint
                        .setReporter(exchange -> {
                            logMessageRdf(makeMessageBox("message NODE => EXTERNAL"),
                                            WonMessage.of(RdfUtils.readDatasetFromString(
                                                            (String) exchange.getIn().getBody(),
                                                            WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE)));
                        });
        interceptMessagesToExternal();
    }

    protected void interceptMessagesToExternal() throws Exception {
        ModelCamelContext context = (ModelCamelContext) camelContext;
        AdviceWith.adviceWith(
                        context.getRouteDefinition("direct:sendToNode"),
                        context,
                        new AdviceWithRouteBuilder() {
                            @Override
                            public void configure() throws Exception {
                                interceptSendToEndpoint("bean:toNodeSender")
                                                .skipSendToOriginalEndpoint()
                                                .bean(messageToSendIntoBody)
                                                .to(toNodeMockEndpoint);
                            }
                        });
    }
}
