package won.cryptography.ssl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko Date: 19.10.2015
 */
public class AliasFromFingerprintGenerator implements AliasGenerator {
    public AliasFromFingerprintGenerator() {
    }

    @Override
    public String generateAlias(final X509Certificate certificate) throws CertificateException {
        String fingerprint = null;
        try {
            fingerprint = digest(certificate.getPublicKey().getEncoded());
        } catch (Exception e) {
            new CertificateException("Alias generation from certificate fingerprint failed", e);
        }
        return fingerprint;
    }

    public String digest(final byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA3-224");
        byte[] hash = md.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void main(String... args) throws Exception {
        System.out.println("digest:" + new AliasFromFingerprintGenerator().digest("digest".getBytes()));
    }
}
