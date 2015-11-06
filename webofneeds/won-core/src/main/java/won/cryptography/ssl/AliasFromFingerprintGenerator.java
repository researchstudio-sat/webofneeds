package won.cryptography.ssl;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko
 * Date: 19.10.2015
 */
public class AliasFromFingerprintGenerator implements AliasGenerator
{
  @Override
  public String generateAlias(final X509Certificate certificate) throws CertificateException {
    String fingerprint = null;
    try {
      fingerprint = DigestUtils.shaHex(certificate.getEncoded());
    } catch (CertificateEncodingException e) {
      new CertificateException("Alias generation from certificate fingerprint failed", e);
    }
    return fingerprint;
  }
}
