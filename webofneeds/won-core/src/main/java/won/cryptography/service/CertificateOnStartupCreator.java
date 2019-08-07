package won.cryptography.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Checks if the node certificate is already present in the specified keystore
 * and creates it if this is not the case. Deprecated, since now the
 * initialization of the key store with default application certificate should
 * be handled by the @see won.cryptography.service.CryptographyService itself.
 */
@Deprecated
public class CertificateOnStartupCreator implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private CryptographyService cryptographyService;

    @Override
    public void afterPropertiesSet() throws IOException {
        String alias = cryptographyService.getDefaultPrivateKeyAlias();
        logger.debug("checking if the node certificate with alias {} is in the keystore", alias);
        if (cryptographyService.containsEntry(alias)) {
            logger.info("entry with alias {} found in the keystore", alias);
            return;
        }
        // no certificate, create it:
        logger.info("node certificate not found under alias {}, creating new one", alias);
        cryptographyService.createNewKeyPair(alias, null);
        logger.info("node certificate created");
    }

    public void setCryptographyService(final CryptographyService cryptographyService) {
        this.cryptographyService = cryptographyService;
    }
}
