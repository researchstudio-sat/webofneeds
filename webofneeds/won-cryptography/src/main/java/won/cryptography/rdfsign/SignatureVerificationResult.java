package won.cryptography.rdfsign;

import won.protocol.message.SignatureReference;

import java.util.*;

/**
 * User: ypanchenko
 * Date: 24.03.2015
 */
public class SignatureVerificationResult {

  private Boolean verificationPassed = null;
  private String message = "";

  private Map<String,List<String>> signedGraphNameToSignatureGraphName = new LinkedHashMap<>();
  private Map<String,Boolean> signatureGraphNameToVerified = new HashMap<>();
  private Map<String,String> signatureGraphNameToSignedGraphName = new HashMap<>();
  private Map<String,String> signatureGraphNameToSignatureValue = new HashMap<>();

  private List<SignatureReference> signatureReferences = new ArrayList<>();


  public void addSignedGraphName(String signedGraphName) {
    if (!signedGraphNameToSignatureGraphName.containsKey(signedGraphName)) {
      signedGraphNameToSignatureGraphName.put(signedGraphName, new ArrayList<String>());
    }
  }


  public void addSignatureData(String signatureGraphName, String signedGraphName, String signatureValue) {
    if (!signedGraphNameToSignatureGraphName.containsKey(signedGraphName)) {
      signedGraphNameToSignatureGraphName.put(signedGraphName, new ArrayList<String>());
    }
    signatureGraphNameToSignedGraphName.put(signatureGraphName, signedGraphName);
    signedGraphNameToSignatureGraphName.get(signedGraphName).add(signatureGraphName);
    signatureGraphNameToSignatureValue.put(signatureGraphName, signatureValue);
  }

  public void setVerificationPassed(String signatureGraphName) {
    signatureGraphNameToVerified.put(signatureGraphName, true);
  }

  public boolean isVerificationPassed(String signatureGraphName) {
    return signatureGraphNameToVerified.get(signatureGraphName);
  }

  public boolean isVerificationPassed() {
    if (verificationPassed != null) {
      return verificationPassed;
    }
    // check if there are any signatures at all
    if (this.signatureGraphNameToSignedGraphName.size() == 0) {
      verificationPassed = false;
      message = "No signatures found";
      return verificationPassed;
    }
    // check whether there is at least one signature for each non-signature graph
    for (String signedName : signedGraphNameToSignatureGraphName.keySet()) {
      if (signedGraphNameToSignatureGraphName.get(signedName).size() < 1) {
        verificationPassed = false;
        message = "No signatures found for " + signedName;
        return verificationPassed;
      }
    }
    // check if all the graphs verify
    for (String sigName : signatureGraphNameToVerified.keySet()) {
      if (!signatureGraphNameToVerified.get(sigName)) {
        verificationPassed = false;
        return verificationPassed;
      }
    }
    // check if referenced signature values are the same as verified signature values
    if (!signatureReferencesValid()) {
      verificationPassed = false;
      return verificationPassed;
    }

    verificationPassed = true;
    return verificationPassed;
  }

  public void setVerificationFailed(String signatureGraphName, String message) {
    signatureGraphNameToVerified.put(signatureGraphName, false);
    this.message = message;
    this.verificationPassed = false;
  }

  public void verificationFailed(String message) {
    this.message = message;
    this.verificationPassed = false;
  }

  public String getMessage() {
    return message;
  }

  public void addSignatureReference(SignatureReference sigReference) {
    this.signatureReferences.add(sigReference);
  }


  public List<SignatureReference> getVerifiedUnreferencedSignaturesAsReferences() {
    List<SignatureReference> newRefs = new ArrayList<SignatureReference>(signedGraphNameToSignatureGraphName.size());
    for (Map.Entry<String, String> entry : signatureGraphNameToSignedGraphName.entrySet()) {
      if (signatureGraphNameToVerified.get(entry.getKey())) {
        if (!referenceExists(entry.getValue(), entry.getKey())) {
          SignatureReference ref = new SignatureReference();
          ref.setSignedGraphUri(entry.getValue());
          ref.setSignatureGraphUri(entry.getKey());
          ref.setSignatureValue(this.signatureGraphNameToSignatureValue.get(entry.getKey()));
          newRefs.add(ref);
        }
      }
    }
    return newRefs;
  }

  private boolean referenceExists(final String signedGraphName, final String sigGraphName) {
    for (SignatureReference ref : signatureReferences) {
      if (signedGraphName.equals(ref.getSignedGraphUri())
        && sigGraphName.equals(ref.getSignatureGraphUri())
        ) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if all signature values in references are equal to the signature values
   * from corresponding signature graphs
   * @return
   */
  private boolean signatureReferencesValid() {
    for (SignatureReference ref : signatureReferences) {
      if (!ref.getSignatureValue().equals(signatureGraphNameToSignatureValue.get(ref.getSignatureGraphUri()))) {
        return false;
      }
    }
    return true;
  }

  public Set<String> getSignatureGraphNames() {
    return this.signatureGraphNameToSignatureValue.keySet();
  }

  public String getSignedGraphName(String signatureGraphName) {
    return this.signatureGraphNameToSignedGraphName.get(signatureGraphName);
  }

}
