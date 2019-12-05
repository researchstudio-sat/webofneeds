package won.owner;

import java.net.URI;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import won.protocol.message.processor.impl.KeyForNewAtomAddingProcessor;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.model.Atom;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.service.WonNodeInformationService;

@ContextConfiguration(locations = { "classpath:/won/owner/OwnerPersistenceTest.xml",
                "classpath:/spring/component/storage/jdbc-storage.xml",
                "classpath:/spring/component/storage/jpabased-rdf-storage.xml",
                "classpath:/spring/component/cryptographyServices.xml",
                "classpath:/spring/component/crypto/owner-crypto.xml"
})
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource
@Transactional
public class OwnerPersistenceTest {
    /**
     * Actual services
     */
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    MessageEventRepository messageEventRepository;
    @Autowired
    SignatureAddingWonMessageProcessor signatureAdder;
    @Autowired
    KeyForNewAtomAddingProcessor atomKeyGeneratorAndAdder;
    /*
     * Mocked services
     */
    @MockBean
    WonNodeInformationService wonNodeInformationService;

    @Test(expected = DataIntegrityViolationException.class)
    public void test_Atom_missing_message_container() {
        Atom atom = new Atom();
        atom.setAtomURI(URI.create("uri:atom"));
        atom.setCreationDate(new Date());
        atomRepository.save(atom);
    }
}
