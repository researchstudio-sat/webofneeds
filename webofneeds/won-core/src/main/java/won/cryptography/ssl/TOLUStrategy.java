package won.cryptography.ssl;

import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.service.TrustStoreService;

import java.lang.invoke.MethodHandles;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Can be useful for development: a certificate will become trusted after
 * application of this strategy, while if already a certificated under the same
 * alias is stored, it will be replaced by this latest certificate. User:
 * ypanchenko Date: 05.08.2015
 */
public class TOLUStrategy implements TrustStrategy {
    private TrustStoreService trustStoreService;
    private AliasGenerator aliasGenerator;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void setTrustStoreService(TrustStoreService trustStoreService) {
        this.trustStoreService = trustStoreService;
    }

    public void setAliasGenerator(AliasGenerator aliasGenerator) {
        this.aliasGenerator = aliasGenerator;
    }

    public boolean isTrusted(final X509Certificate[] x509Certificates, final String authType)
                    throws CertificateException {
        if (x509Certificates == null || x509Certificates.length < 1) {
            return false;
        }
        // extract certificate
        X509Certificate cert = x509Certificates[0];
        // prepare alias
        String alias = aliasGenerator.generateAlias(cert);
        if (trustStoreService.isCertKnown(cert)) {
            return true;
        }
        try {
            trustStoreService.addCertificate(alias, cert, true);
            logger.info("Certificate is added based on TOLU and from now on it is trusted!");
            return true;
        } catch (Exception e) {
            logger.warn("Certificate could not be added as trusted for TOLU for alias " + alias, e);
            return false;
        }
    }
}
