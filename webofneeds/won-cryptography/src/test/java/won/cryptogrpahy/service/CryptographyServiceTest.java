package won.cryptogrpahy.service;

import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import won.cryptography.exception.KeyNotSupportedException;
import won.cryptography.key.KeyInformationExtractorBouncyCastle;
import won.cryptography.service.KeyPairService;

/**
 * User: fsalcher Date: 17.07.2014
 */
public class CryptographyServiceTest {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private ApplicationContext context;

    @Before
    public void init() {
        context = new ClassPathXmlApplicationContext(new String[] { "spring/component/cryptographyServices.xml" });
    }

    @Test
    public void testGenerateKeyPairBrainpoolp384r1() {
        KeyPairService keyPairService = context.getBean("keyPairService", KeyPairService.class);
        KeyPair keypair = keyPairService.generateNewKeyPairInBrainpoolp384r1();
        Assert.assertNotNull(keypair.getPublic());
        Assert.assertNotNull(keypair.getPrivate());
    }

    @Test
    public void testGenerateKeyPairSecp384r1() {
        KeyPairService keyPairService = context.getBean("keyPairService", KeyPairService.class);
        KeyPair keypair = keyPairService.generateNewKeyPairInSecp384r1();
        Assert.assertNotNull(keypair.getPublic());
        Assert.assertNotNull(keypair.getPrivate());
    }

    @Test
    public void testInfoExractorOfKeyPairBrainpoolp384r1() {
        KeyPairService keyPairService = context.getBean("keyPairService", KeyPairService.class);
        KeyPair keypair = keyPairService.generateNewKeyPairInBrainpoolp384r1();
        KeyInformationExtractorBouncyCastle extractor = new KeyInformationExtractorBouncyCastle();
        try {
            Assert.assertEquals("ECDSA", extractor.getAlgorithm(keypair.getPublic()));
            LOGGER.debug("algorithm: " + extractor.getAlgorithm(keypair.getPublic()));
            Assert.assertEquals("brainpoolp384r1", extractor.getCurveID(keypair.getPublic()));
            LOGGER.debug("curveID: " + extractor.getCurveID(keypair.getPublic()));
            Assert.assertNotNull(extractor.getQX(keypair.getPublic()));
            LOGGER.debug("qx: " + extractor.getQX(keypair.getPublic()));
            Assert.assertNotNull(extractor.getQY(keypair.getPublic()));
            LOGGER.debug("qy: " + extractor.getQY(keypair.getPublic()));
        } catch (KeyNotSupportedException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void testInfoExtractorOfKeyPairSecp384r1() {
        KeyPairService keyPairService = context.getBean("keyPairService", KeyPairService.class);
        KeyPair keypair = keyPairService.generateNewKeyPairInSecp384r1();
        KeyInformationExtractorBouncyCastle extractor = new KeyInformationExtractorBouncyCastle();
        try {
            Assert.assertEquals("ECDSA", extractor.getAlgorithm(keypair.getPublic()));
            LOGGER.debug("algorithm: " + extractor.getAlgorithm(keypair.getPublic()));
            Assert.assertEquals("secp384r1", extractor.getCurveID(keypair.getPublic()));
            LOGGER.debug("curveID: " + extractor.getCurveID(keypair.getPublic()));
            Assert.assertNotNull(extractor.getQX(keypair.getPublic()));
            LOGGER.debug("qx: " + extractor.getQX(keypair.getPublic()));
            Assert.assertNotNull(extractor.getQY(keypair.getPublic()));
            LOGGER.debug("qy: " + extractor.getQY(keypair.getPublic()));
        } catch (KeyNotSupportedException ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
