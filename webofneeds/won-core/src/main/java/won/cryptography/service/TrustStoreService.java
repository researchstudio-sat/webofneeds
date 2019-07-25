package won.cryptography.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.service.keystore.FileBasedKeyStoreService;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.security.cert.Certificate;

/**
 * User: ypanchenko Date: 05.08.2015
 */
public class TrustStoreService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private FileBasedKeyStoreService serviceImpl;

    public TrustStoreService(String filePath, String storePW) {
        serviceImpl = new FileBasedKeyStoreService(filePath, storePW);
    }

    public TrustStoreService(File storeFile, String storePW) {
        serviceImpl = new FileBasedKeyStoreService(storeFile, storePW);
    }

    public void init() throws Exception {
        serviceImpl.init();
    }

    public Certificate getCertificate(String alias) {
        logger.debug("Fetching certificate for alias {}", alias);
        return serviceImpl.getCertificate(alias);
    }

    public boolean isCertKnown(Certificate cert) {
        boolean isKnown = serviceImpl.getCertificateAlias(cert) != null;
        logger.debug("Presented certificate is known: {}", isKnown);
        return isKnown;
    }

    // public boolean isAliasKnown(String alias) {
    // return serviceImpl.getCertificate(alias) != null;
    // }
    //
    // public String getCertificateAlias(Certificate cert) {
    // return serviceImpl.getCertificateAlias(cert);
    // }
    public void addCertificate(String alias, Certificate cert, boolean replace) throws IOException {
        logger.debug("adding certificate for alias {}, replace: {}", alias, replace);
        serviceImpl.putCertificate(alias, cert, replace);
    }

    public KeyStore getUnderlyingKeyStore() {
        return serviceImpl.getUnderlyingKeyStore();
    }
}
