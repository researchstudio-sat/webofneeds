package won.node;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
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
import won.node.service.persistence.AtomService;
import won.node.service.persistence.ConnectionService;
import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Atom;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.service.WonNodeInformationService;

@ContextConfiguration(locations = { "classpath:/won/node/common-test-context.xml",
                "classpath:/spring/component/storage/jdbc-storage.xml",
                "classpath:/spring/component/storage/jpabased-rdf-storage.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource
@Transactional
public class PersistenceTest {
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    AtomService atomService;
    @Autowired
    MessageService messageService;
    @Autowired
    ConnectionService connectionService;
    @MockBean
    SocketLookupFromLinkedData socketLookup;
    @MockBean
    WonNodeInformationService wonNodeInformationService;

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
        WonMessage msg = new WonMessage(ds);
        Atom atom = atomService.createAtom(msg);
        URI atomURI = atom.getAtomURI();
        messageService.saveMessage(msg, atom.getAtomURI());
        URI newMessageURI = URI.create("uri:successResponse");
        WonMessage responseMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(msg, true, newMessageURI).build();
        messageService.saveMessage(responseMessage, atom.getAtomURI());
        Atom atom2 = atomService.getAtomRequired(atomURI);
        assertEquals(2, atom2.getMessageContainer().getEvents().size());
    }

    @Test
    public void test_create_two_atoms_and_connect() throws Exception {
        // create an atom, as before
        Dataset ds = createTestDataset("/won/node/test-messages/create-atom.trig");
        WonMessage createMsg = new WonMessage(ds);
        Atom atom = atomService.createAtom(createMsg);
        URI atomURI = atom.getAtomURI();
        messageService.saveMessage(createMsg, atom.getAtomURI());
        URI newMessageURI = URI.create("uri:successResponse");
        WonMessage responseMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(createMsg, true, newMessageURI).build();
        messageService.saveMessage(responseMessage, atom.getAtomURI());
        Atom checkAtom = atomService.getAtomRequired(atomURI);
        assertEquals(2, checkAtom.getMessageContainer().getEvents().size());
        //
        // create another atom
        ds = createTestDataset("/won/node/test-messages/create-atom2.trig");
        WonMessage createMsg2 = new WonMessage(ds);
        Atom atom2 = atomService.createAtom(createMsg2);
        URI atomURI2 = atom2.getAtomURI();
        messageService.saveMessage(createMsg2, atom2.getAtomURI());
        URI successResponse2 = URI.create("uri:successResponse2");
        WonMessage responseMessage2 = WonMessageBuilder
                        .setPropertiesForNodeResponse(createMsg2, true, successResponse2).build();
        messageService.saveMessage(responseMessage2, atom2.getAtomURI());
        checkAtom = atomService.getAtomRequired(atomURI2);
        assertEquals(2, checkAtom.getMessageContainer().getEvents().size());
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
        WonMessage connectMessage = WonMessageBuilder.setMessagePropertiesForConnect(URI.create("uri:connectMessage"),
                        Optional.of(senderSocket),
                        atom.getAtomURI(), atom.getWonNodeURI(),
                        Optional.of(targetSocket), atom2.getAtomURI(),
                        atom2.getWonNodeURI(), "Hey there!").build();
        // processing the message would lead to this call:
        Connection con = connectionService.connectFromOwner(atom.getAtomURI(), atom.getWonNodeURI(), atom2.getAtomURI(),
                        Optional.of(senderSocket),
                        Optional.of(targetSocket), Optional.empty());
        // then it would be stored:
        messageService.saveMessage(connectMessage, con.getConnectionURI());
        // we'd create a response
        URI successForConnect = URI.create("uri:successForConnect");
        WonMessage responseForConnectMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(connectMessage, true, successForConnect).build();
        // and store the response
        messageService.saveMessage(responseForConnectMessage, con.getConnectionURI());
        // let's check:
        assertEquals(2, con.getMessageContainer().getEvents().size());
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
}
