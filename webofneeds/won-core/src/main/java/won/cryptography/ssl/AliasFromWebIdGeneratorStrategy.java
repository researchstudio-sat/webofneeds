package won.cryptography.ssl;

import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import won.cryptography.service.CertificateService;

/**
 * User: ypanchenko Date: 19.10.2015
 */
public class AliasFromWebIdGeneratorStrategy implements AliasGenerator {
  @Override
  public String generateAlias(final X509Certificate certificate) throws CertificateException {
    String alias = null;
    try {
      List<URI> webIds = CertificateService.getWebIdFromSubjectAlternativeNames(certificate);
      alias = webIds.get(0).toString();
    } catch (Exception e) {
      throw new CertificateException("Alias generation from WebID failed", e);
    }
    return alias;
  }
}
