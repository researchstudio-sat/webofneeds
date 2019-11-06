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
import java.security.PublicKey;
import java.security.cert.Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: fsalcher Date: 12.06.2014
 */
public class FileBasedKeyStoreService extends AbstractKeyStoreService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
    private static final String KEY_STORE_TYPE = "UBER";
    // 'UBER' is more secure, 'PKCS12' is supported by all tools, easier for
    // debugging, e.g. when importing keys,
    // therefore temporarily we can use 'PKCS12':
    // private static final String KEY_STORE_TYPE = "PKCS12";
    private String storePW;
    private File storeFile;
    private java.security.KeyStore store;
    private final String provider;
    private final String keyStoreType;

    public FileBasedKeyStoreService(String filePath, String storePW) {
        this(new File(filePath), storePW, PROVIDER_BC, KEY_STORE_TYPE);
    }

    public FileBasedKeyStoreService(File storeFile, String storePW) {
        this(storeFile, storePW, PROVIDER_BC, KEY_STORE_TYPE);
    }

    public FileBasedKeyStoreService(File storeFile, String storePW, String provider, String keyStoreType) {
        this.storeFile = storeFile;
        this.storePW = storePW;
        this.provider = provider;
        this.keyStoreType = keyStoreType;
        logger.info("Using key store file {} with key store type {}, provider {}",
                        new Object[] { storeFile, keyStoreType, provider });
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
            store = (provider == null) ? java.security.KeyStore.getInstance(keyStoreType)
                            : java.security.KeyStore.getInstance(keyStoreType, provider);
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keyStoreType == null) ? 0 : keyStoreType.hashCode());
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        result = prime * result + ((storeFile == null) ? 0 : storeFile.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileBasedKeyStoreService other = (FileBasedKeyStoreService) obj;
        if (keyStoreType == null) {
            if (other.keyStoreType != null)
                return false;
        } else if (!keyStoreType.equals(other.keyStoreType))
            return false;
        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        if (storeFile == null) {
            if (other.storeFile != null)
                return false;
        } else if (!storeFile.equals(other.storeFile))
            return false;
        return true;
    }
}
