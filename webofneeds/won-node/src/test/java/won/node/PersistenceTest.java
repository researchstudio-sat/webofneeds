package won.node;

import java.net.URI;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
public class PersistenceTest {
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    AtomMessageContainerRepository atomMessageContainerRepository;

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
}
