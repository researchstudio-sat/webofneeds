package won.cryptography.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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

    //private final String PRIVATE_KEY_STORE_PREFIX = "PR-";
    //private final String PUBLIC_KEY_STORE_PREFIX = "PU-";

    private KeyPairService keyPairService;

    private CertificateService certificateService;

    private KeyStoreService keyStoreService;

    public CryptographyService (KeyStoreService keyStoreService) {

        Security.addProvider(new BouncyCastleProvider());

        keyPairService = new KeyPairService();
        certificateService = new CertificateService();
        //keyStoreService = new KeyStoreService(new File("keys.jks"));
      this.keyStoreService = keyStoreService;
    }


    public KeyPair createNewKeyPair(BigInteger certNumber, String resourceURI) {


        KeyPair newKeyPair = keyPairService.generateNewKeyPair();
        X509Certificate newCertificate = certificateService.createSelfSignedCertificate(certNumber, newKeyPair,
                                                                                        resourceURI);
        keyStoreService.putKey(resourceURI,
                newKeyPair.getPrivate(), new Certificate[] {newCertificate});

        return newKeyPair;

    }


  public KeyPair createNewKeyPair(String resourceURI) {

    BigInteger certNumber = BigInteger.valueOf(keyStoreService.size() + 1);
    return createNewKeyPair(certNumber, resourceURI);

  }

    public PrivateKey getPrivateKey(String needURI) {
        return keyStoreService.getPrivateKey(needURI);
    }

  public PublicKey getPublicKey(String needURI) {
    return keyStoreService.getPublicKey(needURI);
  }


}
