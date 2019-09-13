package won.cryptography.service;

import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.key.KeyInformationExtractor;

import java.lang.invoke.MethodHandles;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

/**
 * Service responsible for generating key pairs.
 *
 * @author Fabian Salcher
 * @version 2014-07
 */
public class KeyPairService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private KeyPairGenerator keyPairGeneratorBrainpoolp384r1 = new KeyPairGeneratorSpi.ECDSA();
    private org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi keyPairGeneratorSecp384r1 = new org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi.ECDSA();

    public KeyPairService(KeyInformationExtractor keyInformationExtractor) {
        this();
        this.keyInformationExtractor = keyInformationExtractor;
    }

    private KeyInformationExtractor keyInformationExtractor;

    public KeyPairService() {
        try {
            // use the predefined curves
            ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("brainpoolp384r1");
            keyPairGeneratorBrainpoolp384r1.initialize(ecGenSpec, new SecureRandom());
        } catch (Exception e) {
            logger.error("Could not initialize bouncycastle key pair generator for ECDSA brainpoolp384r1");
            throw new IllegalArgumentException(e);
        }
        try {
            ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("secp384r1");
            keyPairGeneratorSecp384r1.initialize(ecGenSpec, new SecureRandom());
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("Could not initialize bouncycastle key pair generator for ECDSA secp384r1");
            throw new IllegalArgumentException(e);
        }
    }

    // TODO make better api for curve support, and ideally also add RSA support...
    public KeyPair generateNewKeyPairInSecp384r1() {
        return keyPairGeneratorSecp384r1.generateKeyPair();
    }

    public KeyPair generateNewKeyPairInBrainpoolp384r1() {
        return keyPairGeneratorBrainpoolp384r1.generateKeyPair();
    }
}
