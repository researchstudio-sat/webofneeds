package won.cryptography.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;

/**
 * User: ypanchenko
 * Date: 05.08.2015
 */
public class TrustStoreService
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private KeyStoreService serviceImpl;

  public TrustStoreService(String filePath, String storePW) {
    serviceImpl = new KeyStoreService(filePath, storePW);
  }

  public TrustStoreService(File storeFile, String storePW) {
    serviceImpl = new KeyStoreService(storeFile, storePW);
  }

  public void init() throws Exception {
    serviceImpl.init();
  }

  public Certificate getCertificate(String alias) {
    return serviceImpl.getCertificate(alias);
  }

  public boolean isCertKnown(Certificate cert) {
    return (serviceImpl.getCertificateAlias(cert) != null);
  }

//  public boolean isAliasKnown(String alias) {
//    return serviceImpl.getCertificate(alias) != null;
//  }
//
//  public String getCertificateAlias(Certificate cert) {
//    return serviceImpl.getCertificateAlias(cert);
//  }

  public void addCertificate(String alias, Certificate cert, boolean replace) throws IOException {
    serviceImpl.putCertificate(alias, cert, replace);
  }

  public KeyStore getUnderlyingKeyStore() {
    return serviceImpl.getUnderlyingKeyStore();

  }

}
