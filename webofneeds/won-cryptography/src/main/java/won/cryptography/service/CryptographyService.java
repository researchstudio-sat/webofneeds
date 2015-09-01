package won.cryptography.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class CryptographyService {

    // ToDo: make class with CryptoConfiguration

    // ToDo: proper error handling

    // ToDo: how to get public key?

    // ToDo: proper initialization of JCE provider!

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    //private final String PRIVATE_KEY_STORE_PREFIX = "PR-";
    //private final String PUBLIC_KEY_STORE_PREFIX = "PU-";

    private KeyPairService keyPairService;

    private CertificateService certificateService;

    private KeyStoreService keyStoreService;

    public CryptographyService (KeyStoreService keyStoreService) {

        //Security.addProvider(new BouncyCastleProvider());
        keyPairService = new KeyPairService();
        certificateService = new CertificateService();
        this.keyStoreService = keyStoreService;
    }


    public KeyPair createNewKeyPair(BigInteger certNumber, String commonName, String webId) {

      KeyPair newKeyPair = keyPairService.generateNewKeyPairInSecp384r1();
        X509Certificate newCertificate = certificateService.createSelfSignedCertificate(certNumber, newKeyPair,
                                                                                        commonName, webId);
      String alias = webId;
      if (alias == null) {
        alias = commonName;
      }
        keyStoreService.putKey(alias,
                newKeyPair.getPrivate(), new Certificate[] {newCertificate});

        return newKeyPair;
    }


  public KeyPair createNewKeyPair(String commonName, String webId) throws IOException {

    BigInteger certNumber = BigInteger.valueOf(keyStoreService.size() + 1);
    return createNewKeyPair(certNumber, commonName, webId);

  }

  public PrivateKey getPrivateKey(String needURI) {
        return keyStoreService.getPrivateKey(needURI);
    }

  public PublicKey getPublicKey(String needURI) {
      return keyStoreService.getPublicKey(needURI);
  }
}
