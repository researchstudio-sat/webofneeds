package won.protocol.message;

/**
 * User: ypanchenko
 * Date: 24.03.2015
 */
public class SignatureReference {

  private String referencerGraphUri;

  private String signedGraphUri;
  private String signatureGraphUri;
  private String signatureValue;

  public SignatureReference(final String referencerGraphUri, final String signedGraphUri,
                            final String signatureGraphUri,
                            final String signatureValue) {
    this.referencerGraphUri = referencerGraphUri;
    this.signedGraphUri = signedGraphUri;
    this.signatureGraphUri = signatureGraphUri;
    this.signatureValue = signatureValue;
  }

  public SignatureReference() {
  }

  public String getReferencerGraphUri() {
    return referencerGraphUri;
  }

  public void setReferencerGraphUri(final String referencerGraphUri) {
    this.referencerGraphUri = referencerGraphUri;
  }

  public String getSignedGraphUri() {
    return signedGraphUri;
  }

  public void setSignedGraphUri(final String signedGraphUri) {
    this.signedGraphUri = signedGraphUri;
  }

  public String getSignatureGraphUri() {
    return signatureGraphUri;
  }

  public void setSignatureGraphUri(final String signatureGraphUri) {
    this.signatureGraphUri = signatureGraphUri;
  }

  public String getSignatureValue() {
    return signatureValue;
  }

  public void setSignatureValue(final String signatureValue) {
    this.signatureValue = signatureValue;
  }

}
