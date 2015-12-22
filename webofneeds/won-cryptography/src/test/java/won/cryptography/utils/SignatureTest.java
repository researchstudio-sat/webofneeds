package won.cryptography.utils;

import org.junit.Before;
import won.cryptography.service.KeyStoreService;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * User: ypanchenko
 * Date: 12.04.2015
 */
public class SignatureTest
{

  PublicKey needPublicKey;
  PublicKey ownerPublicKey;
  PublicKey nodePublicKey;

  PrivateKey needPrivateKey;
  PrivateKey ownerPrivateKey;
  PrivateKey nodePrivateKey;

  @Before
  public void init() {

    //load public  keys:
    File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
    KeyStoreService storeService = new KeyStoreService(keysFile, "temp");
    needPublicKey = storeService.getCertificate(TestSigningUtils.needCertUri).getPublicKey();
    ownerPublicKey = storeService.getCertificate(TestSigningUtils.ownerCertUri).getPublicKey();
    nodePublicKey = storeService.getCertificate(TestSigningUtils.nodeCertUri).getPublicKey();

    needPrivateKey = storeService.getPrivateKey(TestSigningUtils.needCertUri);
    ownerPrivateKey = storeService.getPrivateKey(TestSigningUtils.ownerCertUri);
    nodePrivateKey = storeService.getPrivateKey(TestSigningUtils.nodeCertUri);
  }

//  private String doSign(BigInteger hash, PrivateKey privateKey) {
//
//  }
//
//  private boolean doVerify(String sigString, BigInteger hash, PublicKey publicKey) throws NoSuchProviderException,
//    NoSuchAlgorithmException {
//    Signature sig = Signature.getInstance(WonSigner.SIGNING_ALGORITHM_NAME,
//                                          WonSigner.SIGNING_ALGORITHM_PROVIDER);
//
//
//    byte[] sigBytes = Base64.decodeBase64(sigString);
//    sig.initVerify(publicKey);
//    sig.update(sigData.getHash().toByteArray());
//    //TODO TEMP
//    logger.info(sigData.getHash().toString());
//  }
}
