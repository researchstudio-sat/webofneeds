package won.cryptography.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class KeyStoreService
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
  private static final String KEY_STORE_TYPE = "UBER";
  // 'UBER' is more secure, 'PKCS12' is supported by all tools, easier for debugging, e.g. when importing keys,
  // therefore temporarily we can use 'PKCS12':
  //private static final String KEY_STORE_TYPE = "PKCS12";

  // ToDo: password should really not be here! (FS)
  private String storePW;

  private File storeFile;

  private java.security.KeyStore store;

  private String defaultAlias;

  public KeyStoreService(String filePath, String storePW) {
    this(new File(filePath), storePW);
  }

  public KeyStoreService(File storeFile, String storePW) {
    this.storeFile = storeFile;
    this.storePW = storePW;
  }

  public PrivateKey getPrivateKey(String alias) {

    PrivateKey retrieved = null;

    try {
      // TODO if for storing the needs' keys an individual (e.g. per-user) password is used, then
      // here should be the password of the user/need, not of the store. If not, then here is
      // the password of the store used
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

  //TODO is it OK to expose password like this? or should I specify password in config for each class that needs to
  // access the 'underlayingkeystore' - key managers etc.
  public String getPassword() {
    return storePW;
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

  public synchronized void putKey(String alias, PrivateKey key, Certificate[] certificateChain, boolean replace) throws
    IOException {

//    try {
//      if (!replace && store.containsAlias(alias)) {
//        throw new IOException("Cannot put key - key store already contains entry for " + alias);
//      }
//      // TODO the password here should be the password of the user/need, not of the store
//      store.setKeyEntry(alias, key, storePW.toCharArray(), certificateChain);
//      saveStoreToFile();
//    } catch (Exception e) {
//      throw new IOException("Could not add key of " + alias + " to the key store", e);
//    }
    putEntry(alias, key, certificateChain, null, replace);
  }

  public synchronized void putCertificate(String alias, Certificate certificate, boolean replace) throws
    IOException {

//    try {
//      if (!replace && store.containsAlias(alias)) {
//        throw new IOException("Cannot put certificate - key store already contains entry for " + alias);
//      }
//      store.setCertificateEntry(alias, certificate);
//      saveStoreToFile();
//    } catch (Exception e) {
//      throw new IOException("Could not add certificate of " + alias + " to the key store", e);
//    }
    putEntry(alias, null, null, certificate, replace);
  }


  /**
   * Adding of all the entries into the key store should happen in only one place - in this method - because
   * otherwise there could be concurrency issues when one entry is replace with the other even when the calling
   * method sets replace=false.
   *
   * @param alias
   * @param key
   * @param certificateChain
   * @param certificate
   * @param replace
   * @throws IOException
   */
  private synchronized void putEntry(String alias, PrivateKey key, Certificate[] certificateChain, Certificate
    certificate, boolean replace) throws IOException {

    try {
      if (!replace && store.containsAlias(alias)) {
        throw new IOException("Could not add new entry - key store already contains entry for " + alias);
      }
    } catch (Exception e) {
      throw new IOException("Could not add into the key store entry for " + alias, e);
    }

    try {
      if (alias != null && key != null && certificateChain != null) {
        store.setKeyEntry(alias, key, storePW.toCharArray(), certificateChain);
      } else if (alias != null && certificate != null) {
        store.setCertificateEntry(alias, certificate);
      } else {
        throw new IOException("Could not add entry for " + alias + " to the key store");
      }
      saveStoreToFile();
    } catch (Exception e) {
      throw new IOException("Could not add entry for " + alias + " to the key store", e);
    }

  }



  private synchronized void saveStoreToFile() throws IOException {

    FileOutputStream outputStream = null;

    try {
      outputStream = new FileOutputStream(storeFile);
    } catch (IOException e) {
      logger.error("Could not create key store in file" + storeFile.getName(), e);
      throw e;
    }

    if (outputStream != null) {
      try {
        store.store(outputStream, storePW.toCharArray());
      } catch (Exception e) {
        logger.error("Could not save key store to file" + storeFile.getName(), e);
        throw new IOException(e);
      } finally {
        try {
          outputStream.close();
        } catch (Exception e) {
          logger.error("Error closing stream of file" + storeFile.getName(), e);
          throw e;
        }
      }
    }

  }

  private void loadStoreFromFile() throws Exception {

    FileInputStream inputStream = null;

    try {
      inputStream = new FileInputStream(storeFile);
    } catch (FileNotFoundException e) {
      logger.error("Could not load key store from file" + storeFile.getName(), e);
      throw e;
    }

    if (inputStream != null) {
      try {
        store.load(inputStream, storePW.toCharArray());
      } catch (Exception e) {
        logger.error("Could not load key store from file" + storeFile.getName(), e);
        throw e;
      } finally {
        try {
          inputStream.close();
        } catch (Exception e) {
          logger.error("Error closing stream of file" + storeFile.getName(), e);
          throw e;
        }

      }
    }
  }

  public int size() {
    try {
      return store.size();
    } catch (KeyStoreException e) {
      logger.warn("Could not get size of the key store " + storeFile.getName(), e);
    }
    return 0;
  }

  public void init()
    throws Exception {
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
      throw e;
    }
  }

  public void setDefaultAlias(String defaultAlias) {
    this.defaultAlias = defaultAlias;
  }

  public String getDefaultAlias() {
    return this.defaultAlias;
  }
}
