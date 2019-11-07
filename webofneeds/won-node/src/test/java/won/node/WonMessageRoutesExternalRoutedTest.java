package won.node;

import java.net.URI;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import won.node.test.WonMessageRoutesTestHelper;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.model.Atom;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;

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
 * @author fkleedorfer
 */
public class WonMessageRoutesExternalRoutedTest extends WonMessageRoutesTest {
    @Autowired
    WonMessageRoutesTestHelper helper;

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connection_message__after_handshake() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        WonMessage createAtom2Msg = makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toOwnerMockEndpoint.expectedMessageCount(3);
        toMatcherMockEndpoint.expectedMessageCount(2);
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER2);
        WonMessage socketHintMessage = WonMessageBuilder.setMessagePropertiesForHintToSocket(newMessageURI(), atomURI,
                        socketURI, URI_NODE_1, socketURI2, URI_MATCHER_1, 0.5).build();
        sendFromMatcher(socketHintMessage);
        // start connecting
        WonMessage connectFromExternalMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI2, atomURI2, URI_NODE_1,
                        socketURI, atomURI, URI_NODE_1, "unittest connect")
                        .build();
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        toOwnerMockEndpoint.expectedMessageCount(3);
        sendFromOwner(connectFromExternalMsg, OWNERAPPLICATION_ID_OWNER2);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage connectFromOwnerMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI, atomURI, URI_NODE_1,
                        socketURI2, atomURI2, URI_NODE_1, "unittest connect")
                        .build();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toMatcherMockEndpoint.expectedMessageCount(2);
        sendFromOwner(connectFromOwnerMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Thread t1 = new Thread(() -> helper.doInSeparateTransaction(() -> {
            Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI,
                            socketURI2);
            WonMessage textMsgFromOwner = WonMessageBuilder
                            .setMessagePropertiesForConnectionMessage(newMessageURI(),
                                            con.get().getSocketURI(),
                                            con.get().getConnectionURI(),
                                            con.get().getAtomURI(),
                                            URI_NODE_1,
                                            con.get().getTargetSocketURI(),
                                            con.get().getTargetConnectionURI(),
                                            con.get().getTargetAtomURI(),
                                            URI_NODE_1,
                                            "unittest message from " + socketURI)
                            .build();
            toOwnerMockEndpoint.expectedMessageCount(3);
            toOwnerMockEndpoint.expectedMessagesMatches(
                            or(isMessageAndResponse(textMsgFromOwner), isSuccessResponseTo(textMsgFromOwner),
                                            isMessageAndResponseAndRemoteResponse(textMsgFromOwner)));
            toMatcherMockEndpoint.expectedMessageCount(0);
            sendFromOwner(textMsgFromOwner, OWNERAPPLICATION_ID_OWNER1);
        }));
        t1.start();
        t1.join();
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.CONNECTED);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect__try_force_racecondition() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        WonMessage createAtom2Msg = makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toMatcherMockEndpoint.reset();
        toOwnerMockEndpoint.reset();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toMatcherMockEndpoint.expectedMessageCount(2);
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER2);
        WonMessage socketHintMessage = WonMessageBuilder.setMessagePropertiesForHintToSocket(newMessageURI(), atomURI,
                        socketURI, URI_NODE_1, socketURI2, URI_MATCHER_1, 0.5).build();
        sendFromMatcher(socketHintMessage);
        // start connecting
        WonMessage connectFromExternalMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI2, atomURI2, URI_NODE_1,
                        socketURI, atomURI, URI_NODE_1, "unittest connect")
                        .build();
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage connectFromOwnerMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI, atomURI, URI_NODE_1,
                        socketURI2, atomURI2, URI_NODE_1, "unittest connect 2")
                        .build();
        toOwnerMockEndpoint.expectedMessageCount(6);
        toOwnerMockEndpoint.expectedMessagesMatches(
                        or(
                                        maxOnce(isMessageAndResponse(connectFromExternalMsg)),
                                        maxOnce(isMessageAndResponseAndRemoteResponse(connectFromExternalMsg)),
                                        maxOnce(isSuccessResponseTo(connectFromExternalMsg)),
                                        maxOnce(isMessageAndResponse(connectFromOwnerMsg)),
                                        maxOnce(isMessageAndResponseAndRemoteResponse(connectFromOwnerMsg)),
                                        maxOnce(isSuccessResponseTo(connectFromOwnerMsg))));
        Thread t1 = new Thread(() -> helper.doInSeparateTransaction(
                        () -> sendFromOwner(connectFromExternalMsg, OWNERAPPLICATION_ID_OWNER2)));
        Thread t2 = new Thread(() -> helper
                        .doInSeparateTransaction(() -> sendFromOwner(connectFromOwnerMsg, OWNERAPPLICATION_ID_OWNER1)));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI,
                        socketURI2);
        Connection expected = new Connection();
        expected.setState(ConnectionState.CONNECTED);
        expected.setAtomURI(atomURI);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
        expected.setTargetAtomURI(atomURI2);
        assertConnectionAsExpected(expected, con);
        con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI2,
                        socketURI);
        expected = new Connection();
        expected.setState(ConnectionState.CONNECTED);
        expected.setAtomURI(atomURI2);
        expected.setSocketURI(socketURI2);
        expected.setTargetSocketURI(socketURI);
        expected.setTargetAtomURI(atomURI);
        assertConnectionAsExpected(expected, con);
        executeInSeparateThreadAndWaitForResult(() -> {
            connectionRepository.findByAtomURI(atomURI).stream()
                            .forEach(c -> System.out.println("con of " + atomURI + ":" + c));
            connectionRepository.findByAtomURI(atomURI2).stream()
                            .forEach(c -> System.out.println("con of " + atomURI2 + ":" + c));
        });
    }

    @Test
    @Transactional
    public void test_pessimistic_lock__separate_transactions() throws InterruptedException {
        AtomicLong waited = new AtomicLong(-1);
        AtomicBoolean expectedValueFound = new AtomicBoolean(false);
        executeInSeparateThreadAndWaitForResult(() -> {
            Atom atom = new Atom();
            atom.setAtomURI(URI_ATOM_1_ON_NODE_1);
            atom.setState(AtomState.ACTIVE);
            AtomMessageContainer container = new AtomMessageContainer(atom, atom.getAtomURI());
            atom = atomRepository.saveAndFlush(atom);
        });
        Thread parallelThread1 = new Thread(() -> helper.doInSeparateTransaction(() -> {
            Atom b = atomRepository.findOneByAtomURIForUpdate(URI_ATOM_1_ON_NODE_1);
            System.out.println("read in other thread: " + b);
        }));
        parallelThread1.start();
        Thread.sleep(100);
        Thread parallelThread2 = new Thread(() -> helper.doInSeparateTransaction(() -> {
            Atom a = atomRepository.findOneByAtomURIForUpdate(URI_ATOM_1_ON_NODE_1);
            System.out.println("read in yet another thread: " + a);
            System.out.println("blocking...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            a.setCreationDate(new Date(1));
            atomRepository.save(a);
        }));
        parallelThread2.start();
        Thread.sleep(100);
        Thread parallelThread3 = new Thread(() -> helper.doInSeparateTransaction(() -> {
            System.out.println("acquiring exclusive lock...");
            long now = System.currentTimeMillis();
            Atom b = atomRepository.findOneByAtomURIForUpdate(URI_ATOM_1_ON_NODE_1);
            System.out.println("read in the third thread: " + b);
            System.out.println("waited: " + (System.currentTimeMillis() - now));
            waited.set(System.currentTimeMillis() - now);
            expectedValueFound.set(b.getCreationDate().getTime() == 1);
        }));
        parallelThread3.start();
        System.out.println("waiting for parallel threads to finish");
        parallelThread1.join();
        parallelThread2.join();
        parallelThread3.join();
        Assert.assertTrue("thread should have been blocked at least 500ms", waited.get() > 500);
        Assert.assertTrue("thread3 did not get update by thread2", expectedValueFound.get());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_conversation() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        WonMessage createAtom2Msg = makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toOwnerMockEndpoint.expectedMessageCount(3);
        toMatcherMockEndpoint.expectedMessageCount(2);
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER2);
        WonMessage socketHintMessage = WonMessageBuilder.setMessagePropertiesForHintToSocket(newMessageURI(), atomURI,
                        socketURI, URI_NODE_1, socketURI2, URI_MATCHER_1, 0.5).build();
        sendFromMatcher(socketHintMessage);
        // start connecting
        WonMessage connectFromExternalMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI2, atomURI2, URI_NODE_1,
                        socketURI, atomURI, URI_NODE_1, "unittest connect")
                        .build();
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        toMatcherMockEndpoint.expectedMessageCount(0);
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(connectFromExternalMsg),
                        isMessageAndResponseAndRemoteResponse(connectFromExternalMsg),
                        isSuccessResponseTo(connectFromExternalMsg)));
        sendFromOwner(connectFromExternalMsg, OWNERAPPLICATION_ID_OWNER2);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage connectFromOwnerMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI, atomURI, URI_NODE_1,
                        socketURI2, atomURI2, URI_NODE_1, "unittest connect")
                        .build();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(connectFromOwnerMsg),
                        isMessageAndResponseAndRemoteResponse(connectFromOwnerMsg),
                        isSuccessResponseTo(connectFromOwnerMsg)));
        toMatcherMockEndpoint.expectedMessageCount(2);
        sendFromOwner(connectFromOwnerMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI,
                        socketURI2);
        WonMessage textMsgFromOwner = WonMessageBuilder
                        .setMessagePropertiesForConnectionMessage(newMessageURI(),
                                        con.get().getSocketURI(),
                                        con.get().getConnectionURI(),
                                        con.get().getAtomURI(),
                                        URI_NODE_1,
                                        con.get().getTargetSocketURI(),
                                        con.get().getTargetConnectionURI(),
                                        con.get().getTargetAtomURI(),
                                        URI_NODE_1,
                                        "unittest message from " + socketURI)
                        .build();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(textMsgFromOwner),
                        isMessageAndResponseAndRemoteResponse(textMsgFromOwner),
                        isSuccessResponseTo(textMsgFromOwner)));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(textMsgFromOwner, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        textMsgFromOwner = WonMessageBuilder
                        .setMessagePropertiesForConnectionMessage(newMessageURI(),
                                        con.get().getSocketURI(),
                                        con.get().getConnectionURI(),
                                        con.get().getAtomURI(),
                                        URI_NODE_1,
                                        con.get().getTargetSocketURI(),
                                        con.get().getTargetConnectionURI(),
                                        con.get().getTargetAtomURI(),
                                        URI_NODE_1,
                                        "unittest message 2 from " + socketURI)
                        .build();
        sendFromOwner(textMsgFromOwner, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(textMsgFromOwner),
                        isMessageAndResponseAndRemoteResponse(textMsgFromOwner),
                        isSuccessResponseTo(textMsgFromOwner)));
        toMatcherMockEndpoint.expectedMessageCount(0);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage textMsgFromOtherOwner = WonMessageBuilder
                        .setMessagePropertiesForConnectionMessage(newMessageURI(),
                                        con.get().getTargetSocketURI(),
                                        con.get().getTargetConnectionURI(),
                                        con.get().getTargetAtomURI(),
                                        URI_NODE_1,
                                        con.get().getSocketURI(),
                                        con.get().getConnectionURI(),
                                        con.get().getAtomURI(),
                                        URI_NODE_1,
                                        "unittest message from " + socketURI2)
                        .build();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(textMsgFromOtherOwner),
                        isMessageAndResponseAndRemoteResponse(textMsgFromOtherOwner),
                        isSuccessResponseTo(textMsgFromOtherOwner)));
        sendFromOwner(textMsgFromOtherOwner, OWNERAPPLICATION_ID_OWNER2);
        toMatcherMockEndpoint.expectedMessageCount(0);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.CONNECTED);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_close__after_conversation() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        WonMessage createAtom2Msg = makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toOwnerMockEndpoint.expectedMessageCount(3);
        toMatcherMockEndpoint.expectedMessageCount(2);
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER2);
        WonMessage socketHintMessage = WonMessageBuilder.setMessagePropertiesForHintToSocket(newMessageURI(), atomURI,
                        socketURI, URI_NODE_1, socketURI2, URI_MATCHER_1, 0.5).build();
        sendFromMatcher(socketHintMessage);
        // start connecting
        WonMessage connectFromExternalMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI2, atomURI2, URI_NODE_1,
                        socketURI, atomURI, URI_NODE_1, "unittest connect")
                        .build();
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        toOwnerMockEndpoint.expectedMessageCount(3);
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(connectFromExternalMsg, OWNERAPPLICATION_ID_OWNER2);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage connectFromOwnerMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI, atomURI, URI_NODE_1,
                        socketURI2, atomURI2, URI_NODE_1, "unittest connect")
                        .build();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toMatcherMockEndpoint.expectedMessageCount(2);
        sendFromOwner(connectFromOwnerMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI,
                        socketURI2);
        WonMessage textMsgFromOwner = WonMessageBuilder
                        .setMessagePropertiesForConnectionMessage(newMessageURI(),
                                        con.get().getSocketURI(),
                                        con.get().getConnectionURI(),
                                        con.get().getAtomURI(),
                                        URI_NODE_1,
                                        con.get().getTargetSocketURI(),
                                        con.get().getTargetConnectionURI(),
                                        con.get().getTargetAtomURI(),
                                        URI_NODE_1,
                                        "unittest message from " + socketURI)
                        .build();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(textMsgFromOwner),
                        isMessageAndResponseAndRemoteResponse(textMsgFromOwner),
                        isSuccessResponseTo(textMsgFromOwner)));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(textMsgFromOwner, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        textMsgFromOwner = WonMessageBuilder
                        .setMessagePropertiesForConnectionMessage(newMessageURI(),
                                        con.get().getSocketURI(),
                                        con.get().getConnectionURI(),
                                        con.get().getAtomURI(),
                                        URI_NODE_1,
                                        con.get().getTargetSocketURI(),
                                        con.get().getTargetConnectionURI(),
                                        con.get().getTargetAtomURI(),
                                        URI_NODE_1,
                                        "unittest message 2 from " + socketURI)
                        .build();
        sendFromOwner(textMsgFromOwner, OWNERAPPLICATION_ID_OWNER1);
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(textMsgFromOwner),
                        isMessageAndResponseAndRemoteResponse(textMsgFromOwner),
                        isSuccessResponseTo(textMsgFromOwner)));
        toMatcherMockEndpoint.expectedMessageCount(0);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage textMsgFromOtherOwner = WonMessageBuilder
                        .setMessagePropertiesForConnectionMessage(newMessageURI(),
                                        con.get().getTargetSocketURI(),
                                        con.get().getTargetConnectionURI(),
                                        con.get().getTargetAtomURI(),
                                        URI_NODE_1,
                                        con.get().getSocketURI(),
                                        con.get().getConnectionURI(),
                                        con.get().getAtomURI(),
                                        URI_NODE_1,
                                        "unittest message from " + socketURI2)
                        .build();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(textMsgFromOtherOwner),
                        isMessageAndResponseAndRemoteResponse(textMsgFromOtherOwner),
                        isSuccessResponseTo(textMsgFromOtherOwner)));
        sendFromOwner(textMsgFromOtherOwner, OWNERAPPLICATION_ID_OWNER2);
        toMatcherMockEndpoint.expectedMessageCount(0);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage closeMsg = WonMessageBuilder.setMessagePropertiesForClose(newMessageURI(),
                        con.get().getSocketURI(),
                        con.get().getTargetSocketURI(),
                        WonMessageDirection.FROM_OWNER,
                        "unittest close!").build();
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(closeMsg),
                        isMessageAndResponseAndRemoteResponse(closeMsg),
                        isSuccessResponseTo(closeMsg)));
        toMatcherMockEndpoint.expectedMessageCount(2);
        sendFromOwner(closeMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.CLOSED);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect__after_hint() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        WonMessage createAtom2Msg = makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        // set minimal expectations just so we can expect something and subsequently
        // reset expectations
        toOwnerMockEndpoint.expectedMessageCount(3);
        toMatcherMockEndpoint.expectedMessageCount(2);
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
        // set new expectations for hint
        WonMessage socketHintMessage = WonMessageBuilder.setMessagePropertiesForHintToSocket(newMessageURI(), atomURI,
                        socketURI, URI_NODE_1, socketURI2, URI_MATCHER_1, 0.5).build();
        sendFromMatcher(socketHintMessage);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage connectMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI, atomURI, URI_NODE_1,
                        socketURI2, atomURI2, URI_NODE_1, "unittest connect")
                        .build();
        // expectations for connect
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(or(
                        isMessageAndResponse(connectMsg),
                        isMessageAndResponseAndRemoteResponse(connectMsg),
                        isSuccessResponseTo(connectMsg)));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(connectMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.REQUEST_SENT);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, socketURI2);
        assertConnectionAsExpected(expected, con);
        Assert.assertNotNull("connection in state REQUEST_SENT should already have a target connection",
                        con.get().getTargetConnectionURI());
        expected = new Connection();
        expected.setState(ConnectionState.REQUEST_RECEIVED);
        expected.setSocketURI(socketURI2);
        expected.setTargetSocketURI(socketURI);
        con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI2, socketURI);
        assertConnectionAsExpected(expected, con);
        Assert.assertNull("connection in state REQUEST_RECEIVED should not have a target connection yet",
                        con.get().getTargetConnectionURI());
    }

    @Test
    @Commit // @Rollback would't work as camel still commits
    public void test_connect() throws Exception {
        URI atomURI = newAtomURI();
        URI socketURI = URI.create(atomURI.toString() + "#socket1");
        URI atomURI2 = newAtomURI();
        URI socketURI2 = URI.create(atomURI2.toString() + "#socket1");
        prepareMockitoStubs(atomURI, socketURI, atomURI2, socketURI2);
        WonMessage createAtom1Msg = makeCreateAtomMessage(atomURI,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        WonMessage createAtom2Msg = makeCreateAtomMessage(atomURI2,
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
        // set expectations
        toOwnerMockEndpoint.expectedMessageCount(2);
        toOwnerMockEndpoint.expectedMessagesMatches(
                        or(isMessageAndResponse(createAtom1Msg), isMessageAndResponse(createAtom2Msg)));
        toMatcherMockEndpoint.expectedMessageCount(2);
        toMatcherMockEndpoint.expectedMessagesMatches(
                        or(isAtomCreatedNotification(atomURI), isAtomCreatedNotification(atomURI2)));
        // send message
        sendFromOwner(createAtom1Msg, OWNERAPPLICATION_ID_OWNER1);
        sendFromOwner(createAtom2Msg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        WonMessage connectMsg = WonMessageBuilder.setMessagePropertiesForConnect(
                        newMessageURI(), socketURI, atomURI, URI_NODE_1,
                        socketURI2, atomURI2, URI_NODE_1, "unittest connect")
                        .build();
        // expectations for connect
        toOwnerMockEndpoint.expectedMessageCount(3);
        toOwnerMockEndpoint.expectedMessagesMatches(doesResponseContainSender(),
                        or(
                                        isMessageAndResponse(connectMsg),
                                        isSuccessResponseTo(connectMsg),
                                        isMessageAndResponseAndRemoteResponse(connectMsg)));
        toMatcherMockEndpoint.expectedMessageCount(0);
        sendFromOwner(connectMsg, OWNERAPPLICATION_ID_OWNER1);
        assertMockEndpointsSatisfiedAndReset(toOwnerMockEndpoint, toMatcherMockEndpoint);
        Connection expected = new Connection();
        expected.setState(ConnectionState.REQUEST_SENT);
        expected.setSocketURI(socketURI);
        expected.setTargetSocketURI(socketURI2);
        Optional<Connection> con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI, socketURI2);
        assertConnectionAsExpected(expected, con);
        Assert.assertNotNull("connection in state REQUEST_SENT should already have a target connection",
                        con.get().getTargetConnectionURI());
        expected = new Connection();
        expected.setState(ConnectionState.REQUEST_RECEIVED);
        expected.setSocketURI(socketURI2);
        expected.setTargetSocketURI(socketURI);
        con = connectionRepository.findOneBySocketURIAndTargetSocketURI(socketURI2, socketURI);
        assertConnectionAsExpected(expected, con);
        Assert.assertNull("connection in state REQUEST_RECEIVED should not have a target connection yet",
                        con.get().getTargetConnectionURI());
    }
}
