package won.cryptography.rdfsign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import won.protocol.message.WonSignatureData;

/**
 * User: ypanchenko Date: 24.03.2015
 */
public class SignatureVerificationState {
    private Boolean verificationPassed = null;
    private String message = "";
    private final Map<String, List<String>> signedGraphNameToSignatureGraphName = new LinkedHashMap<>();
    private final Map<String, Boolean> signatureGraphNameToVerified = new HashMap<>();
    private final Map<String, List<String>> signatureGraphNameToSignedGraphName = new HashMap<>();
    private final Map<String, String> signatureGraphNameToSignatureValue = new HashMap<>();
    private final List<WonSignatureData> signatures = new ArrayList<>();

    public void addSignedGraphName(String signedGraphName) {
        if (!signedGraphNameToSignatureGraphName.containsKey(signedGraphName)) {
            signedGraphNameToSignatureGraphName.put(signedGraphName, new ArrayList<>());
        }
    }

    public void addSignatureData(WonSignatureData wonSignatureData) {
        signatures.add(wonSignatureData);
        for (String signed : wonSignatureData.getSignedGraphUris()) {
            List<String> sigs = signedGraphNameToSignatureGraphName.get(signed);
            if (sigs == null) {
                sigs = new ArrayList<>();
            }
            sigs.add(wonSignatureData.getSignatureUri());
            signedGraphNameToSignatureGraphName.put(signed, sigs);
        }
        signatureGraphNameToSignedGraphName.put(wonSignatureData.getSignatureUri(),
                        wonSignatureData.getSignedGraphUris());
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

    public List<String> getSignedGraphNames(String signatureGraphName) {
        return this.signatureGraphNameToSignedGraphName.get(signatureGraphName);
    }
}
