package won.cryptography.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko Date: 19.10.2015
 */
public interface AliasGenerator {
    public String generateAlias(X509Certificate certificate) throws CertificateException;
}
