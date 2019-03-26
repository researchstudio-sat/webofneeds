package won.cryptography.service.keystore;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractKeyStoreService implements KeyStoreService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
  private static final String KEY_STORE_TYPE = "UBER";

  // 'UBER' is more secure, 'PKCS12' is supported by all tools, easier for
  // debugging, e.g. when importing keys,
  // therefore temporarily we can use 'PKCS12':
  // private static final String KEY_STORE_TYPE = "PKCS12";

  /*
   * (non-Javadoc)
   * 
   * @see won.cryptography.service.KeyStoreService#getPrivateKey(java.lang.String)
   */
  @Override
  public PrivateKey getPrivateKey(String alias) {
    try {
      return (PrivateKey) getUnderlyingKeyStore().getKey(alias, getPassword().toCharArray());
    } catch (Exception e) {
      logger.warn("Could not retrieve key for " + alias + " from keystore", e);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see won.cryptography.service.KeyStoreService#getPublicKey(java.lang.String)
   */
  @Override
  public PublicKey getPublicKey(String alias) {
    Certificate cert = getCertificate(alias);
    if (cert == null) {
      logger.warn("No certificate found for alias {}", alias);
      return null;
    }
    return cert.getPublicKey();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * won.cryptography.service.KeyStoreService#getCertificate(java.lang.String)
   */
  @Override
  public Certificate getCertificate(String alias) {
    try {
      return getUnderlyingKeyStore().getCertificate(alias);
    } catch (KeyStoreException e) {
      logger.warn("could not retrieve certificate for alias " + alias, e);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * won.cryptography.service.KeyStoreService#getCertificateAlias(java.security.
   * cert.Certificate)
   */
  @Override
  public String getCertificateAlias(Certificate cert) {
    String retrieved = null;

    try {
      retrieved = getUnderlyingKeyStore().getCertificateAlias(cert);
    } catch (Exception e) {
      logger.warn("No alias found for certificate", e);
    }

    return retrieved;

  }

  /*
   * (non-Javadoc)
   * 
   * @see won.cryptography.service.KeyStoreService#putKey(java.lang.String,
   * java.security.PrivateKey, java.security.cert.Certificate[], boolean)
   */
  @Override
  public synchronized void putKey(String alias, PrivateKey key, Certificate[] certificateChain, boolean replace)
      throws IOException {

    putEntry(alias, key, certificateChain, null, replace);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * won.cryptography.service.KeyStoreService#putCertificate(java.lang.String,
   * java.security.cert.Certificate, boolean)
   */
  @Override
  public synchronized void putCertificate(String alias, Certificate certificate, boolean replace) throws IOException {

    putEntry(alias, null, null, certificate, replace);
  }

  /**
   * Adding of all the entries into the key store should happen in only one place
   * - in this method - because otherwise there could be concurrency issues when
   * one entry is replace with the other even when the calling method sets
   * replace=false.
   *
   * @param alias
   * @param key
   * @param certificateChain
   * @param certificate
   * @param replace
   */
  protected synchronized void putEntry(String alias, PrivateKey key, Certificate[] certificateChain,
      Certificate certificate, boolean replace) {
    KeyStore store = getUnderlyingKeyStore();
    try {
      if (!replace && store.containsAlias(alias)) {
        return;
      }
    } catch (Exception e) {
      throw new RuntimeException("Error checking if key with alias '" + alias + "' is in the keystore", e);
    }

    try {
      if (alias == null) {
        throw new RuntimeException("Could not add keystore entry: provided alias is null");
      }
      if (alias != null && key != null && certificateChain != null) {
        store.setKeyEntry(alias, key, getPassword().toCharArray(), certificateChain);
      } else if (alias != null && certificate != null) {
        store.setCertificateEntry(alias, certificate);
      } else {
        throw new RuntimeException(
            "Could not add keystore entry: neither key and cert chain nor certificate is provided");
      }
      persistStore();
    } catch (Exception e) {
      throw new RuntimeException("Could not add entry for " + alias + " to the key store", e);
    }

  }

  protected abstract void persistStore() throws Exception;

  public boolean containsEntry(String alias) {
    try {
      return getUnderlyingKeyStore().containsAlias(alias);
    } catch (KeyStoreException e) {
      return false;
    }
  }
}
