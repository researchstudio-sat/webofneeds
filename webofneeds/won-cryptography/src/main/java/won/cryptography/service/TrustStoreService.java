package won.cryptography.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

  public TrustStoreService(String filePath) {
    serviceImpl = new KeyStoreService(filePath);
  }

  public TrustStoreService(File storeFile) {
    serviceImpl = new KeyStoreService(storeFile);
  }

  public void init(){
    serviceImpl.init();
  }

  public Certificate getCertificate(String alias) {
    return serviceImpl.getCertificate(alias);
  }

  public boolean isCertKnown(Certificate cert) {
    return (serviceImpl.getCertificateAlias(cert) != null);
  }

  public String getCertificateAlias(Certificate cert) {
    return serviceImpl.getCertificateAlias(cert);
  }

  public void addCertificate(String alias, Certificate cert) {
    serviceImpl.putCertificate(alias, cert);
  }

  public KeyStore getUnderlyingKeyStore() {
    return serviceImpl.getUnderlyingKeyStore();

  }

}
