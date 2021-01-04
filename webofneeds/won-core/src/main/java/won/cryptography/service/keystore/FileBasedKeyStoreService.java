package won.cryptography.service.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.Certificate;

import java.util.Objects;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.service.BCProvider;

/**
 * User: fsalcher Date: 12.06.2014
 */
public class FileBasedKeyStoreService extends AbstractKeyStoreService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String KEY_STORE_TYPE = "UBER";
    // 'UBER' is more secure, 'PKCS12' is supported by all tools, easier for
    // debugging, e.g. when importing keys,
    // therefore temporarily we can use 'PKCS12':
    // private static final String KEY_STORE_TYPE = "PKCS12";
    private String storePW;
    private File storeFile;
    private java.security.KeyStore store;
    private final String keyStoreType;

    public FileBasedKeyStoreService(String filePath, String storePW) {
        this(new File(filePath), storePW, KEY_STORE_TYPE);
    }

    public FileBasedKeyStoreService(File storeFile, String storePW) {
        this(storeFile, storePW, KEY_STORE_TYPE);
    }

    public FileBasedKeyStoreService(File storeFile, String storePW,
                    String keyStoreType) {
        this.storeFile = storeFile;
        this.storePW = storePW;
        this.keyStoreType = keyStoreType;
        logger.info("Using key store file {} with key store type {}, provider {}",
                        new Object[] { storeFile, keyStoreType, BCProvider.getInstance().getClass().getName() });
    }

    /*
     * (non-Javadoc)
     * @see won.cryptography.service.KeyStoreService#getPrivateKey(java.lang.String)
     */
    @Override
    public PrivateKey getPrivateKey(String alias) {
        PrivateKey retrieved = null;
        try {
            retrieved = (PrivateKey) store.getKey(alias, storePW.toCharArray());
        } catch (Exception e) {
            logger.warn("Could not retrieve key for " + alias + " from ks " + storeFile.getName(), e);
        }
        return retrieved;
    }

    private String makeCacheKeyForPrivateKey(String alias) {
        return "PK " + hashCode() + alias;
    }

    /*
     * (non-Javadoc)
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
     * @see won.cryptography.service.KeyStoreService#getPassword()
     */
    @Override
    public String getPassword() {
        return storePW;
    }

    /*
     * (non-Javadoc)
     * @see
     * won.cryptography.service.KeyStoreService#getCertificate(java.lang.String)
     */
    @Override
    public Certificate getCertificate(String alias) {
        Certificate retrieved = null;
        try {
            retrieved = store.getCertificate(alias);
        } catch (Exception e) {
            logger.warn("No certificate found for alias " + alias, e);
        }
        return retrieved;
    }

    private Serializable makeCacheKeyForCertificate(String alias) {
        return "CERT " + hashCode() + alias;
    }

    /*
     * (non-Javadoc)
     * @see
     * won.cryptography.service.KeyStoreService#getCertificateAlias(java.security.
     * cert.Certificate)
     */
    @Override
    public String getCertificateAlias(Certificate cert) {
        String retrieved = null;
        try {
            retrieved = store.getCertificateAlias(cert);
        } catch (Exception e) {
            logger.warn("No alias found for certificate", e);
        }
        return retrieved;
    }

    /*
     * (non-Javadoc)
     * @see won.cryptography.service.KeyStoreService#getUnderlyingKeyStore()
     */
    @Override
    public KeyStore getUnderlyingKeyStore() {
        return this.store;
    }

    /*
     * (non-Javadoc)
     * @see
     * won.cryptography.service.KeyStoreService#putCertificate(java.lang.String,
     * java.security.cert.Certificate, boolean)
     */
    @Override
    public synchronized void putCertificate(String alias, Certificate certificate, boolean replace) throws IOException {
        putEntry(alias, null, null, certificate, replace);
    }

    protected synchronized void persistStore() throws Exception {
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(storeFile);
        } catch (IOException e) {
            logger.error("Could not create key store in file " + storeFile.getName(), e);
            throw e;
        }
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

    private void loadStoreFromFile() throws Exception {
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(storeFile);
        } catch (FileNotFoundException e) {
            logger.error("Could not load key store from file" + storeFile.getName(), e);
            throw e;
        }
        try {
            store.load(inputStream, storePW.toCharArray());
        } catch (Exception e) {
            logger.error("Could not load key store from file " + storeFile.getName(), e);
            throw e;
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                logger.error("Error closing stream of file " + storeFile.getName(), e);
                throw e;
            }
        }
    }

    public void init() throws Exception {
        try {
            try {
                store = java.security.KeyStore.getInstance(keyStoreType, BCProvider.getInstance());
            } catch (Exception e) {
                // try again with standard provider resolution
                try {
                    store = java.security.KeyStore.getInstance(keyStoreType);
                } catch (Exception e2) {
                    logger.error("Error initializing key store with provider {}: {} - fallback to default provider failed, too (see stacktrace below).",
                                    BCProvider.getInstance().getClass(), e.getMessage());
                    throw e2;
                }
            }
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FileBasedKeyStoreService that = (FileBasedKeyStoreService) o;
        return Objects.equals(storeFile, that.storeFile) &&
                        Objects.equals(keyStoreType, that.keyStoreType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeFile, keyStoreType);
    }
}
