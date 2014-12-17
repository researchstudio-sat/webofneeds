package won.cryptography.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.security.Key;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class CryptographyService {

    // ToDo: make class with CryptoConfiguration

    // ToDo: fix logging

    // ToDo: proper error handling

    // ToDo: how to get public key?

    // ToDo: proper initialization of JCE provider!

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String PRIVATE_KEY_STORE_PREFIX = "PR-";
    private final String PUBLIC_KEY_STORE_PREFIX = "PU-";

    private KeyPairService keyPairService;

    private CertificateService certificateService;

    private KeyStoreService keyStoreService;

    public CryptographyService () {

        Security.addProvider(new BouncyCastleProvider());

        keyPairService = new KeyPairService();
        certificateService = new CertificateService();
        keyStoreService = new KeyStoreService(new File("keys.jks"));
    }


    public KeyPair createNewNeedKeyPair(URI needURI) {


        KeyPair newKeyPair = keyPairService.generateNewKeyPair();
        X509Certificate newCertificate = certificateService.createSelfSignedCertificate(newKeyPair);
        keyStoreService.putKey(PRIVATE_KEY_STORE_PREFIX + needURI.toString(),
                newKeyPair.getPrivate(), new Certificate[] {newCertificate});
        keyStoreService.putKey(PUBLIC_KEY_STORE_PREFIX + needURI.toString(),
                newKeyPair.getPublic(), new Certificate[] {newCertificate});

        return newKeyPair;

    }

    public Key getNeedPrivateKey (URI needURI) {

        return keyStoreService.getKey(needURI.toString());

    }


}
