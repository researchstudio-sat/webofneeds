package won.cryptography.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.*;

/**
 * User: fsalcher
 * Date: 28.05.2014
 */
public class KeyPairService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public KeyPair generateNewKeyPair() {

        Security.addProvider(new BouncyCastleProvider());

        KeyPair pair = null;

        try {

            // use the predefined curves
            ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("brainpoolp384r1");

            KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "BC");
            g.initialize(ecGenSpec, new SecureRandom());
            pair = g.generateKeyPair();

        } catch (Exception e) {
            logger.warn("An error occurred!", e);
        }

        return pair;
    }

}
