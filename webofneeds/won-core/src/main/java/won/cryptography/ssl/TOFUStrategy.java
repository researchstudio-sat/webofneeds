package won.cryptography.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.cryptography.service.TrustStoreService;

/**
 * Trust on first use strategy: if certificate is already known and trusted (from previous communication) - trust it.
 * If not yet in the store, and we can successfully add it to the store (no alias collision based on the provided alias
 * generator) - trust it. Otherwise - don't trust. For example if we have already the certificate under the same alias
 * in the store - we don't trust it because we already trust that other certificate (alias should represent the
 * certificate owner unique id, e.g. for server it is usually the authority, for client can be anything).
 *
 * User: ypanchenko
 * Date: 05.08.2015
 */
public class TOFUStrategy implements TrustStrategy
{
  private TrustStoreService trustStoreService;
  private AliasGenerator aliasGenerator = new AliasFromFingerprintGenerator();

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public void setTrustStoreService(TrustStoreService trustStoreService) {
    this.trustStoreService = trustStoreService;
  }

  // this parameter is specific to TOFU, since it has to store the newly encountered certificate into the trust-store
  // under some alias
  public void setAliasGenerator(AliasGenerator aliasGenerator) {
    this.aliasGenerator = aliasGenerator;
  }


  public boolean isTrusted(final X509Certificate[] x509Certificates, final String authType) throws
    CertificateException {

    if (x509Certificates == null || x509Certificates.length < 1) {
      return false;
    }
    // extract certificate
    X509Certificate cert = x509Certificates[0];
    // prepare alias
    String alias =  aliasGenerator.generateAlias(cert);

    if (trustStoreService.isCertKnown(cert)) {
      return true;
    }

    try {
      trustStoreService.addCertificate(alias, cert, false);
      logger.info("Certificate for " + alias + " is added based on TOFU and from now on it is trusted!");
      return true;
    } catch (Exception e) {
      logger.warn("Certificate could not be added as trusted for TOFU for alias " + alias, e);
      return false;
    }

  }


}
