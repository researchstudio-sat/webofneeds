package won.cryptography.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.ssl.TrustStrategy;

/**
 * Can be useful for testing User: ypanchenko Date: 15.10.2015
 */
public class TrustNooneStrategy implements TrustStrategy {
    @Override
    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        return false;
    }
}
