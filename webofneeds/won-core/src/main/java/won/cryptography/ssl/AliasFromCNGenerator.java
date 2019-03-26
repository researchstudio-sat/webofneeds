package won.cryptography.ssl;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import sun.security.x509.X500Name;

/**
 * User: ypanchenko Date: 19.10.2015
 */
public class AliasFromCNGenerator implements AliasGenerator {
  @Override
  public String generateAlias(final X509Certificate certificate) throws CertificateException {
    String alias = null;
    try {
      X500Name dnName = new X500Name(certificate.getSubjectDN().getName());
      alias = dnName.getCommonName();
    } catch (IOException e) {
      throw new CertificateException("SubjectDN problem - cannot generate alias", e);
    }
    if (alias == null || alias.isEmpty()) {
      throw new CertificateException("CN is null - cannot accept as alias");
    }
    return alias;
  }
}
