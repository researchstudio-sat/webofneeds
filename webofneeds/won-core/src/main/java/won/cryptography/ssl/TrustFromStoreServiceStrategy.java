package won.cryptography.ssl;

import org.apache.http.ssl.TrustStrategy;
import won.cryptography.service.TrustStoreService;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko Date: 07.10.2015
 */
public class TrustFromStoreServiceStrategy implements TrustStrategy {

  private TrustStoreService trustStoreService;

  public void setTrustStoreService(final TrustStoreService trustStoreService) {
    this.trustStoreService = trustStoreService;
  }

  @Override
  public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
    if (chain.length == 0 || chain[0] == null) {
      return false;
    }
    X509Certificate serverCert = chain[0];
    // consider trusted if we already know this certificate
    if (trustStoreService.isCertKnown(serverCert)) {
      return true;
    }
    return false;
  }
}
