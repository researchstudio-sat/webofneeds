package won.node;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import won.node.service.persistence.AtomService;
import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Atom;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.AtomState;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;

@ContextConfiguration(locations = { "classpath:/won/node/common-test-context.xml",
                "classpath:/spring/component/storage/jdbc-storage.xml",
                "classpath:/spring/component/storage/jpabased-rdf-storage.xml" })
@TestPropertySource
@RunWith(SpringJUnit4ClassRunner.class)
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
    JpaTransactionManager txManager;

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
        Atom atom2 = atomRepository.findOneByAtomURI(atomURI);
        assertEquals(2, atom2.getMessageContainer().getEvents().size());
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
