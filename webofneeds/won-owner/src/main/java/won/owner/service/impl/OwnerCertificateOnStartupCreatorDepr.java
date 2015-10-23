package won.owner.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import won.cryptography.service.CryptographyService;

/**
 * User: ypanchenko
 * Date: 17.08.2015
 */

/**
 * Checks if the owner certificate is already present in the specified keystore
 * and creates it if this is not the case.
 */
public class OwnerCertificateOnStartupCreatorDepr implements InitializingBean
{
//  private final Logger logger  = LoggerFactory.getLogger(getClass());
//
//
//  private String alias;
//  private KeyStoreService keyStoreService;
//
//  private CertificateService certificateService = new CertificateService();
//  private KeyPairService keyPairService = new KeyPairService();
//
//  private CryptographyService cryptographyService;
//
//
//  @Override
//  public void afterPropertiesSet() throws Exception {
//    logger.debug("checking if the owner certificate with alias {} is in the keystore", alias);
//    Certificate cert = keyStoreService.getCertificate(alias);
//    cryptographyService.getPublicKey();
//    if (cert != null) {
//      logger.info("owner certificate with alias {} found in the keystore", alias);
//      return;
//    }
//    //no certificate, create it:
//    logger.info("owner certificate not found under alias {}, creating new one", alias);
//    KeyPair pair = keyPairService.generateNewKeyPairInSecp384r1();
//    BigInteger serialNumber = BigInteger.valueOf(1);
//    cert = certificateService.createSelfSignedCertificate(serialNumber, pair, URI.create(alias).getAuthority(), null);
//    keyStoreService.putKey(alias, pair.getPrivate(), new Certificate[]{cert});
//    cryptographyService.createNewKeyPair(serialNumber, alias, alias);
//
//    logger.info("owner certificate created");
//  }
//
////  public void setAlias(String alias) {
////    this.alias = alias;
////  }
//
//  public void setKeyStoreService(KeyStoreService keyStoreService) {
//    this.keyStoreService = keyStoreService;
//    this.alias = keyStoreService.getDefaultAlias();
//  }

  private final Logger logger  = LoggerFactory.getLogger(getClass());

  private CryptographyService cryptographyService;


  @Override
  public void afterPropertiesSet() throws Exception {
    String alias = cryptographyService.getDefaultPrivateKeyAlias();
    logger.debug("checking if the owner certificate with alias {} is in the keystore", alias);
    if (cryptographyService.containsEntry(alias)) {
      logger.info("entry with alias {} found in the keystore", alias);
      return;
    }
    //no certificate, create it:
    logger.info("owner certificate not found under alias {}, creating new one", alias);
    cryptographyService.createNewKeyPair(alias, null);
    logger.info("owner certificate created");
  }

  public void setCryptographyService(final CryptographyService cryptographyService) {
    this.cryptographyService = cryptographyService;
  }
}