package won.protocol.message;

import java.util.List;

/**
 * User: ypanchenko Date: 24.03.2015
 */
public class WonSignatureData {
    private List<String> signedGraphUris;
    private String signatureUri;
    private String signatureValue;
    private String hash;
    private String publicKeyFingerprint;
    private String verificationCertificateUri;

    public WonSignatureData(final List<String> signedGraphUris, final String signatureUri, final String signatureValue,
                    final String hash, final String publicKeyFingerprint, final String verificationCertificateUri) {
        this.signedGraphUris = signedGraphUris;
        this.signatureUri = signatureUri;
        this.signatureValue = signatureValue;
        this.hash = hash;
        this.publicKeyFingerprint = publicKeyFingerprint;
        this.verificationCertificateUri = verificationCertificateUri;
    }

    public List<String> getSignedGraphUris() {
        return signedGraphUris;
    }

    public void setSignedGraphUri(final List<String> signedGraphUris) {
        this.signedGraphUris = signedGraphUris;
    }

    public String getSignatureUri() {
        return signatureUri;
    }

    public void setSignatureUri(final String signatureUri) {
        this.signatureUri = signatureUri;
    }

    public String getSignatureValue() {
        return signatureValue;
    }

    public void setSignatureValue(final String signatureValue) {
        this.signatureValue = signatureValue;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(final String hash) {
        this.hash = hash;
    }

    public String getPublicKeyFingerprint() {
        return publicKeyFingerprint;
    }

    public void setPublicKeyFingerprint(final String publicKeyFingerprint) {
        this.publicKeyFingerprint = publicKeyFingerprint;
    }

    public String getVerificationCertificateUri() {
        return verificationCertificateUri;
    }

    public void setVerificationCertificateUri(final String verificationCertificateUri) {
        this.verificationCertificateUri = verificationCertificateUri;
    }
}
