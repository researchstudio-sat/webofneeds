package won.owner;

import java.net.URI;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import won.owner.model.KeystoreHolder;
import won.owner.model.KeystorePasswordHolder;
import won.owner.model.User;
import won.owner.model.UserAtom;
import won.owner.repository.UserAtomRepository;
import won.owner.repository.UserRepository;
import won.owner.service.impl.KeystorePasswordUtils;
import won.owner.test.OwnerPersistanceTestHelper;
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
    @Autowired
    OwnerPersistanceTestHelper helper;
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
    @Autowired
    UserAtomRepository userAtomRepository;
    @Autowired
    UserRepository userRepository;
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

    @Test
    public void test_delete_UserAtom() throws Exception {
        URI atomUri = URI.create("some:/atom.uri");
        String email = "user@example.com";
        createUserWithAtom(atomUri, email);
        Thread t1 = new Thread(() -> helper.doInSeparateTransaction(
                        () -> createUserWithAtom(atomUri, email)));
        Thread t2 = new Thread(() -> helper.doInSeparateTransaction(
                        () -> {
                            User sameUser = userRepository.findByUsername(email);
                            UserAtom sameAtom = userAtomRepository.findByAtomUri(atomUri);
                            sameUser.removeUserAtom(sameAtom);
                            userAtomRepository.delete(sameAtom);
                        }));
        t1.start();
        t1.join();
        t2.start();
        t2.join();
    }

    private void createUserWithAtom(URI atomUri, String email) {
        UserAtom a = new UserAtom();
        a.setUri(atomUri);
        a = userAtomRepository.save(a);
        String password = "password";
        String role = "SOMEROLE";
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = new User(email, passwordEncoder.encode(password), role);
        user.setEmail(email);
        user.setAcceptedTermsOfService(true); // transfer only available when flag is set therefore we can just set
                                              // this
                                              // to true (i think)
        KeystorePasswordHolder keystorePassword = new KeystorePasswordHolder();
        // generate a password for the keystore and save it in the database, encrypted
        // with a symmetric key
        // derived from the user's password
        keystorePassword.setPassword(
                        KeystorePasswordUtils.generatePassword(KeystorePasswordUtils.KEYSTORE_PASSWORD_BYTES),
                        password);
        // keystorePassword = keystorePasswordRepository.save(keystorePassword);
        // generate the keystore for the user
        KeystoreHolder keystoreHolder = new KeystoreHolder();
        try {
            // create the keystore if it doesnt exist yet
            keystoreHolder.getKeystore(keystorePassword.getPassword(password));
        } catch (Exception e) {
            throw new IllegalStateException("could not create keystore for user " + email);
        }
        // keystoreHolder = keystoreHolderRepository.save(keystoreHolder);
        user.setKeystorePasswordHolder(keystorePassword);
        user.setKeystoreHolder(keystoreHolder);
        user = userRepository.save(user);
        user.addUserAtom(a);
        user = userRepository.save(user);
    }
}
