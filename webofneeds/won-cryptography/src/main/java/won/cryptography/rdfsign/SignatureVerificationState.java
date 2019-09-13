package won.cryptography.rdfsign;

import won.protocol.message.WonSignatureData;

import java.util.*;

/**
 * User: ypanchenko Date: 24.03.2015
 */
public class SignatureVerificationState {
    private Boolean verificationPassed = null;
    private String message = "";
    private final Map<String, List<String>> signedGraphNameToSignatureGraphName = new LinkedHashMap<>();
    private final Map<String, Boolean> signatureGraphNameToVerified = new HashMap<>();
    private final Map<String, String> signatureGraphNameToSignedGraphName = new HashMap<>();
    private final Map<String, String> signatureGraphNameToSignatureValue = new HashMap<>();
    private final List<WonSignatureData> signatures = new ArrayList<>();

    public void addSignedGraphName(String signedGraphName) {
        if (!signedGraphNameToSignatureGraphName.containsKey(signedGraphName)) {
            signedGraphNameToSignatureGraphName.put(signedGraphName, new ArrayList<>());
        }
    }

    public void addSignatureData(WonSignatureData wonSignatureData) {
        signatures.add(wonSignatureData);
        if (!signedGraphNameToSignatureGraphName.containsKey(wonSignatureData.getSignedGraphUri())) {
            signedGraphNameToSignatureGraphName.put(wonSignatureData.getSignedGraphUri(), new ArrayList<>());
        }
        signatureGraphNameToSignedGraphName.put(wonSignatureData.getSignatureUri(),
                        wonSignatureData.getSignedGraphUri());
        signedGraphNameToSignatureGraphName.get(wonSignatureData.getSignedGraphUri())
                        .add(wonSignatureData.getSignatureUri());
        signatureGraphNameToSignatureValue.put(wonSignatureData.getSignatureUri(),
                        wonSignatureData.getSignatureValue());
    }

    public boolean isVerificationPassed() {
        if (this.verificationPassed != null) {
            return this.verificationPassed;
        }
        // check whether there is at least one signature for each non-signature graph
        for (String signedName : signedGraphNameToSignatureGraphName.keySet()) {
            if (signedGraphNameToSignatureGraphName.get(signedName).size() < 1) {
                this.verificationPassed = false;
                message = "No signatures found for " + signedName;
                return this.verificationPassed;
            }
        }
        verificationPassed = true;
        return this.verificationPassed;
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
