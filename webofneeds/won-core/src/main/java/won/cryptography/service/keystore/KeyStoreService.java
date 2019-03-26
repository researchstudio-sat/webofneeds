package won.cryptography.service.keystore;

import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

public interface KeyStoreService {

  PrivateKey getPrivateKey(String alias);

  /**
   * Returns the public key of the certificate stored under the specified alias or
   * null if no such certificat is found.
   *
   * @param alias
   * @return
   */
  PublicKey getPublicKey(String alias);

  // TODO is it OK to expose password like this? or should I specify password in
  // config for each class that needs to
  // access the 'underlayingkeystore' - key managers etc.
  String getPassword();

  Certificate getCertificate(String alias);

  String getCertificateAlias(Certificate cert);

  /**
   * Useful method if TrustManager has to be configured with this key store
   *
   */
  KeyStore getUnderlyingKeyStore();

  void putKey(String alias, PrivateKey key, Certificate[] certificateChain, boolean replace) throws IOException;

  void putCertificate(String alias, Certificate certificate, boolean replace) throws IOException;

}