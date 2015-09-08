package won.cryptography.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class KeyStoreService
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
  //private static final String KEY_STORE_TYPE = "UBER";
  // 'UBER' is more secure, 'PKCS12' is supported by all tools, easier for debugging, e.g. when importing keys,
  // therefore temporarily we can use 'PKCS12':
  private static final String KEY_STORE_TYPE = "PKCS12";

  // ToDo: password should really not be here! (FS)
  private final String storePW = "temp";

  // ToDo: load from config file (FS)

  private File storeFile;

  private java.security.KeyStore store;

  private String defaultAlias;

  public KeyStoreService(String filePath) {
    this(new File(filePath));
  }

  public KeyStoreService(File storeFile) {
    this.storeFile = storeFile;
  }

  public PrivateKey getPrivateKey(String alias) {

    PrivateKey retrieved = null;

    try {
      retrieved = (PrivateKey) store.getKey(alias, storePW.toCharArray());
    } catch (Exception e) {
      logger.warn("Could not retrieve key for " + alias + " from ks " + storeFile.getName(), e);
    }

    return retrieved;

  }

  /**
   * Returns the public key of the certificate stored under the specified alias or null
   * if no such certificat is found.
   *
   * @param alias
   * @return
   */
  public PublicKey getPublicKey(String alias) {
    Certificate cert = getCertificate(alias);
    if (cert == null) {
      logger.warn("No certificate found for alias {}", alias);
      return null;
    }
    return cert.getPublicKey();
  }

  public Certificate getCertificate(String alias) {

    Certificate retrieved = null;

    try {
      retrieved = store.getCertificate(alias);
    } catch (Exception e) {
      logger.warn("No certificate found for alias " + alias, e);
    }

    return retrieved;

  }

  public String getCertificateAlias(Certificate cert) {

    String retrieved = null;

    try {
      retrieved = store.getCertificateAlias(cert);
    } catch (Exception e) {
      logger.warn("No alias found for certificate", e);
    }

    return retrieved;

  }

  /**
   * Useful method if TrustManager has to be configured with this key store
   *
   */
  public KeyStore getUnderlyingKeyStore() {

    return this.store;

  }

  public synchronized void putKey(String alias, Key key, Certificate[] certificateChain) {

    try {
      store.setKeyEntry(alias, key, storePW.toCharArray(), certificateChain);
      saveStoreToFile();
    } catch (Exception e) {
      logger.error("Could not add key of " + alias + " to the key store", e);
    }

  }

  public synchronized void putCertificate(String alias, Certificate certificate) {

    try {
      store.setCertificateEntry(alias, certificate);
      saveStoreToFile();
    } catch (Exception e) {
      logger.error("Could not add certificate of " + alias + " to the key store", e);
    }

  }

  private synchronized void saveStoreToFile() {

    FileOutputStream outputStream = null;

    try {
      outputStream = new FileOutputStream(storeFile);
    } catch (IOException e) {
      logger.error("Could not create key store in file" + storeFile.getName(), e);
    }

    if (outputStream != null) {
      try {

        store.store(outputStream, storePW.toCharArray());

      } catch (Exception e) {
        logger.error("Could not save key store to file" + storeFile.getName(), e);
      } finally {
        try {
          outputStream.close();
        } catch (Exception e) {
          logger.error("Error closing stream of file" + storeFile.getName(), e);
        }
      }
    }

  }

  private void loadStoreFromFile() {

    FileInputStream inputStream = null;

    try {
      inputStream = new FileInputStream(storeFile);
    } catch (FileNotFoundException e) {
      logger.error("Could not load key store from file" + storeFile.getName(), e);
    }

    if (inputStream != null) {
      try {
        store.load(inputStream, storePW.toCharArray());
      } catch (Exception e) {
        logger.error("Could not load key store from file" + storeFile.getName(), e);
      } finally {
        try {
          inputStream.close();
        } catch (Exception e) {
          logger.error("Error closing stream of file" + storeFile.getName(), e);
        }

      }
    }
  }

  public int size() {
    try {
      return store.size();
    } catch (KeyStoreException e) {
      //TODO proper logging
      logger.warn("Could not get size of the key store " + storeFile.getName(), e);
    }
    return 0;
  }

  public void init() {
    try {
      store = java.security.KeyStore.getInstance(KEY_STORE_TYPE, PROVIDER_BC);
      logger.debug("KEYSTORE: " + store);

      if (storeFile == null || !storeFile.exists() || !storeFile.isFile())
        store.load(null, null);
      else {
        loadStoreFromFile();
      }
    } catch (Exception e) {
      logger.error("Error initializing key store " + storeFile.getName(), e);
    }
  }

  public void setDefaultAlias(String defaultAlias) {
    this.defaultAlias = defaultAlias;
  }

  public String getDefaultAlias() {
    return this.defaultAlias;
  }
}
