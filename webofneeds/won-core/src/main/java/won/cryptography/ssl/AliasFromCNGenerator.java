package won.cryptography.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

/**
 * User: ypanchenko Date: 19.10.2015
 */
public class AliasFromCNGenerator implements AliasGenerator {
    @Override
    public String generateAlias(final X509Certificate certificate) throws CertificateException {
        String alias;
        X500Principal dnName = new X500Principal(certificate.getSubjectDN().getName());
        alias = dnName.getName();
        if (alias == null || alias.isEmpty()) {
            throw new CertificateException("CN is null - cannot accept as alias");
        }
        return alias;
    }
}
