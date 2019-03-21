package won.cryptography.rdfsign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.WonSignatureData;

import java.util.*;

/**
 * User: ypanchenko Date: 24.03.2015
 */
public class SignatureVerificationState {

  private Boolean verificationPassed = null;
  private String message = "";

  private Map<String, List<String>> signedGraphNameToSignatureGraphName = new LinkedHashMap<>();
  private Map<String, Boolean> signatureGraphNameToVerified = new HashMap<>();
  private Map<String, String> signatureGraphNameToSignedGraphName = new HashMap<>();
  private Map<String, String> signatureGraphNameToSignatureValue = new HashMap<>();

  private List<WonSignatureData> signatures = new ArrayList<>();
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public void addSignedGraphName(String signedGraphName) {
    if (!signedGraphNameToSignatureGraphName.containsKey(signedGraphName)) {
      signedGraphNameToSignatureGraphName.put(signedGraphName, new ArrayList<String>());
    }
  }

  public void addSignatureData(WonSignatureData wonSignatureData) {
    signatures.add(wonSignatureData);
    if (!signedGraphNameToSignatureGraphName.containsKey(wonSignatureData.getSignedGraphUri())) {
      signedGraphNameToSignatureGraphName.put(wonSignatureData.getSignedGraphUri(), new ArrayList<String>());
    }
    signatureGraphNameToSignedGraphName.put(wonSignatureData.getSignatureUri(), wonSignatureData.getSignedGraphUri());
    signedGraphNameToSignatureGraphName.get(wonSignatureData.getSignedGraphUri())
        .add(wonSignatureData.getSignatureUri());
    signatureGraphNameToSignatureValue.put(wonSignatureData.getSignatureUri(), wonSignatureData.getSignatureValue());
  }

  public boolean isVerificationPassed() {
    if (verificationPassed != null) {
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

  public List<WonSignatureData> getSignatures() {
    return signatures;
  }

  public Set<String> getSignatureGraphNames() {
    return this.signatureGraphNameToSignatureValue.keySet();
  }

  public String getSignedGraphName(String signatureGraphName) {
    return this.signatureGraphNameToSignedGraphName.get(signatureGraphName);
  }

}
