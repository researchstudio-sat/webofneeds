package won.cryptography.ssl;

import org.apache.http.ssl.TrustStrategy;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko Date: 21.10.2015
 */
public class TrustAnyCertificateStrategy implements TrustStrategy {
  @Override
  public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
    return chain.length >= 1;
  }
}
