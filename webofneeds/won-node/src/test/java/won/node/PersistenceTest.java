package won.node;

import org.apache.camel.CamelContext;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import won.node.service.linkeddata.lookup.SocketLookupFromLinkedData;
import won.node.service.nodeconfig.URIService;
import won.node.service.persistence.AtomService;
import won.node.service.persistence.ConnectionService;
import won.node.service.persistence.DataDerivationService;
import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.impl.KeyForNewAtomAddingProcessor;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.message.processor.impl.WonMessageSignerVerifier;
import won.protocol.model.*;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(locations = { "classpath:/won/node/PersistenceTest.xml",
                "classpath:/spring/component/storage/jdbc-storage.xml",
                "classpath:/spring/component/storage/jpabased-rdf-storage.xml",
                "classpath:/spring/component/cryptographyServices.xml",
                "classpath:/spring/component/crypto/node-crypto.xml"
})
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource
@Transactional
public class PersistenceTest {
    protected static AtomicInteger counter = new AtomicInteger(0);
    /**
     * Actual services
     */
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    DataDerivationService dataDerivationService;
    @Autowired
    AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    MessageEventRepository messageEventRepository;
    @Autowired
    AtomService atomService;
    @Autowired
    MessageService messageService;
    @Autowired
    ConnectionService connectionService;
    @MockBean
    URIService uriService;
    @Autowired
    SignatureAddingWonMessageProcessor signatureAdder;
    @Autowired
    KeyForNewAtomAddingProcessor atomKeyGeneratorAndAdder;
    /*
     * Mocked services
     */
    @MockBean
    SocketLookupFromLinkedData socketLookup;
    @MockBean
    WonNodeInformationService wonNodeInformationService;
    @MockBean
    CamelContext camelContext;
    @MockBean
    LinkedDataSource linkedDataSource;

    @Before
    public void setUp() {
        Mockito.when(uriService.isAtomURI(any())).thenReturn(true);
        Mockito.when(uriService.getAtomResourceURIPrefix()).then(x -> "uri:/node/resource/atom");
        Mockito.when(uriService.getResourceURIPrefix()).then(x -> "uri:/node/resource");
        Mockito.when(uriService.createSysInfoGraphURIForAtomURI(any())).thenCallRealMethod();
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void test_Atom_missing_message_container() {
        Atom atom = new Atom();
        atom.setAtomURI(URI.create("uri:atom"));
        atom.setCreationDate(new Date());
        atomRepository.save(atom);
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void test_Atom_missing_state() {
        Atom atom = new Atom();
        URI atomUri = URI.create("uri:atom");
        atom.setAtomURI(atomUri);
        atom.setCreationDate(new Date());
        AtomMessageContainer mc = new AtomMessageContainer(atom, atom.getAtomURI());
        atomMessageContainerRepository.save(mc);
        mc.setParentUri(atomUri);
        atom.setMessageContainer(mc);
        atomRepository.save(atom);
    }

    @Test
    public void test_Atom_ok() {
        URI atomUri = URI.create("uri:atom");
        Atom atom = new Atom();
        atom.setState(AtomState.ACTIVE);
        atom.setAtomURI(atomUri);
        atom.setCreationDate(new Date());
        AtomMessageContainer mc = new AtomMessageContainer(atom, atom.getAtomURI());
        atomMessageContainerRepository.save(mc);
        mc.setParentUri(atomUri);
        atom.setMessageContainer(mc);
        atomRepository.save(atom);
    }

    @Test
    public void test_create_Atom_from_message() throws Exception {
        Dataset ds = createTestDataset("/won/node/test-messages/create-atom.trig");
        WonMessage msg = WonMessage.of(ds);
        Atom atom = atomService.createAtom(msg);
        URI atomURI = atom.getAtomURI();
        messageService.saveMessage(msg, atom.getAtomURI());
        WonMessage responseMessage = WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(msg)
                        .success()
                        .build();
        messageService.saveMessage(responseMessage, atom.getAtomURI());
        Atom atom2 = atomService.getAtomRequired(atomURI);
        assertEquals(2, messageEventRepository.findByParentURI(atom2.getAtomURI()).size());
    }

    @Test
    public void test_create_two_atoms_and_connect() throws Exception {
        // create an atom, as before
        Dataset ds = createTestDataset("/won/node/test-messages/create-atom.trig");
        WonMessage createMsg = WonMessage.of(ds);
        Atom atom = atomService.createAtom(createMsg);
        URI atomURI = atom.getAtomURI();
        messageService.saveMessage(createMsg, atom.getAtomURI());
        WonMessage responseMessage = WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(createMsg)
                        .success()
                        .build();
        messageService.saveMessage(responseMessage, atom.getAtomURI());
        Atom checkAtom = atomService.getAtomRequired(atomURI);
        assertEquals(2, messageEventRepository.findByParentURI(checkAtom.getAtomURI()).size());
        //
        // create another atom
        ds = createTestDataset("/won/node/test-messages/create-atom2.trig");
        WonMessage createMsg2 = WonMessage.of(ds);
        Atom atom2 = atomService.createAtom(createMsg2);
        URI atomURI2 = atom2.getAtomURI();
        messageService.saveMessage(createMsg2, atom2.getAtomURI());
        WonMessage responseMessage2 = WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(createMsg2)
                        .success()
                        .build();
        messageService.saveMessage(responseMessage2, atom2.getAtomURI());
        checkAtom = atomService.getAtomRequired(atomURI2);
        assertEquals(2, messageEventRepository.findByParentURI(checkAtom.getAtomURI()).size());
        //
        // simulate a connect from atom to atom2
        // we'll need a connection uri from the mocked service
        Mockito.when(wonNodeInformationService
                        .generateConnectionURI(URI.create("https://node.matchat.org/won/resource")))
                        .thenReturn(URI.create("uri:newConnection"));
        URI senderSocket = URI.create(atom.getAtomURI().toString() + "#chatSocket");
        URI targetSocket = URI.create(atom2.getAtomURI().toString() + "#chatSocket");
        Mockito.when(socketLookup.getCapacity(senderSocket)).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.getCapacity(targetSocket)).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.isCompatible(senderSocket, targetSocket)).thenReturn(true);
        Mockito.when(socketLookup.isCompatible(targetSocket, senderSocket)).thenReturn(true);
        Mockito.when(socketLookup.getCapacityOfType(any())).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.isCompatibleSocketTypes(any(), any())).thenReturn(true);
        Mockito.when(socketLookup.getSocketConfig(any()))
                        .thenReturn(Optional.of(getChatSocketDefForDerivation()));
        WonMessage connectMessage = prepareFromOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(senderSocket).recipient(targetSocket)
                        .content().text("Hey there!")
                        .build());
        // processing the message would lead to this call:
        Mockito.when(wonNodeInformationService.generateConnectionURI(atom.getAtomURI()))
                        .thenReturn(URI.create("uri:newconnection1"));
        Connection con = connectionService.connectFromOwner(connectMessage);
        // then it would be stored:
        messageService.saveMessage(connectMessage, con.getConnectionURI());
        // we'd create a response
        WonMessage responseForConnectMessage = prepareFromExternalOwner(WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(connectMessage)
                        .fromConnection(con.getConnectionURI())
                        .success()
                        .build());
        // and store the response
        messageService.saveMessage(responseForConnectMessage, con.getConnectionURI());
        // let's check:
        assertEquals(2, messageEventRepository.findByParentURI(con.getConnectionURI()).size());
        Set<URI> messages = messageEventRepository.findByParentURI(con.getConnectionURI()).stream()
                        .map(mic -> mic.getMessageURI()).collect(Collectors.toSet());
    }

    @Test
    public void test_create_two_atoms_and_connect_use_same_message_in_both_connections() throws Exception {
        // create an atom, as before
        Dataset ds = createTestDataset("/won/node/test-messages/create-atom.trig");
        WonMessage createMsg = prepareFromOwner(WonMessage.of(ds));
        Atom atom = atomService.createAtom(createMsg);
        URI atomURI = atom.getAtomURI();
        messageService.saveMessage(createMsg, atom.getAtomURI());
        WonMessage responseMessage = prepareFromSystem(WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(createMsg)
                        .success()
                        .build());
        messageService.saveMessage(responseMessage, atom.getAtomURI());
        Atom checkAtom = atomService.getAtomRequired(atomURI);
        assertEquals(2, messageEventRepository.findByParentURI(checkAtom.getAtomURI()).size());
        //
        // create another atom
        ds = createTestDataset("/won/node/test-messages/create-atom2.trig");
        WonMessage createMsg2 = prepareFromOwner(WonMessage.of(ds));
        Atom atom2 = atomService.createAtom(createMsg2);
        URI atomURI2 = atom2.getAtomURI();
        messageService.saveMessage(createMsg2, atom2.getAtomURI());
        WonMessage responseMessage2 = prepareFromSystem(WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(createMsg2)
                        .success()
                        .build());
        messageService.saveMessage(responseMessage2, atom2.getAtomURI());
        checkAtom = atomService.getAtomRequired(atomURI2);
        assertEquals(2, messageEventRepository.findByParentURI(checkAtom.getAtomURI()).size());
        //
        // simulate a connect from atom to atom2
        // we'll need a connection uri from the mocked service
        Mockito.when(wonNodeInformationService
                        .generateConnectionURI(URI.create("https://node.matchat.org/won/resource")))
                        .thenReturn(URI.create("uri:newConnection"));
        URI senderSocket = URI.create(atom.getAtomURI().toString() + "#chatSocket");
        URI targetSocket = URI.create(atom2.getAtomURI().toString() + "#chatSocket");
        Mockito.when(socketLookup.getCapacity(senderSocket)).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.getCapacity(targetSocket)).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.isCompatible(senderSocket, targetSocket)).thenReturn(true);
        Mockito.when(socketLookup.isCompatible(targetSocket, senderSocket)).thenReturn(true);
        Mockito.when(socketLookup.getCapacityOfType(any())).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.isCompatibleSocketTypes(any(), any())).thenReturn(true);
        Mockito.when(socketLookup.getSocketConfig(any()))
                        .thenReturn(Optional.of(getChatSocketDefForDerivation()));
        WonMessage connectMessage = prepareFromOwner(WonMessageBuilder
                        .connect()
                        .sockets().sender(senderSocket).recipient(targetSocket)
                        .content().text("Hey there!")
                        .build());
        // processing the message would lead to this call:
        Mockito.when(wonNodeInformationService.generateConnectionURI(any()))
                        .then(invocation -> newConnectionURI(invocation.getArgument(0, URI.class)));
        Connection con = connectionService.connectFromOwner(connectMessage);
        // then it would be stored:
        messageService.saveMessage(connectMessage, con.getConnectionURI());
        // we'd create a response
        WonMessage responseForConnectMessage = prepareFromSystem(WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(connectMessage)
                        .fromConnection(con.getConnectionURI())
                        .success()
                        .build());
        // and store the response
        messageService.saveMessage(responseForConnectMessage, con.getConnectionURI());
        // let's check:
        assertEquals(2, messageEventRepository.findByParentURI(con.getConnectionURI()).size());
        Set<URI> messages = messageEventRepository.findByParentURI(con.getConnectionURI()).stream()
                        .map(mic -> mic.getMessageURI()).collect(Collectors.toSet());
        Connection remoteCon = connectionService.connectFromNode(connectMessage);
        messageService.saveMessage(connectMessage, remoteCon.getConnectionURI());
        WonMessage responseForConnectMessage2 = prepareFromSystem(WonMessageBuilder
                        .response()
                        .respondingToMessageFromOwner(connectMessage)
                        .fromConnection(remoteCon.getConnectionURI())
                        .success()
                        .build());
        messageService.saveMessage(responseForConnectMessage2, remoteCon.getConnectionURI());
        assertEquals(2, messageEventRepository.findByParentURI(con.getConnectionURI()).size());
        Set<URI> messages2 = messageEventRepository.findByParentURI(remoteCon.getConnectionURI()).stream()
                        .map(mic -> mic.getMessageURI()).collect(Collectors.toSet());
    }

    private SocketDefinition getChatSocketDefForDerivation() {
        return new SocketDefinition() {
            @Override
            public URI getSocketURI() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public Optional<URI> getSocketDefinitionURI() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public Set<URI> getDerivationProperties() {
                return Collections.emptySet();
            }

            @Override
            public Set<URI> getInverseDerivationProperties() {
                return Collections.emptySet();
            }

            @Override
            public boolean isCompatibleWith(SocketDefinition other) {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public boolean isAutoOpen() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public Optional<Integer> getCapacity() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public Set<URI> getInconsistentProperties() {
                throw new UnsupportedOperationException("not implemented");
            }
        };
    }

    private Dataset createTestDataset(String resourceName) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        Dataset dataset = DatasetFactory.createGeneral();
        dataset.begin(ReadWrite.WRITE);
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();
        dataset.commit();
        return dataset;
    }

    protected URI newConnectionURI(URI atomUri) {
        return URI.create(atomUri.toString() + "/c/connection-" + counter.incrementAndGet());
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
}
