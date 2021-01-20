package won.cryptography.webid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import won.cryptography.rdfsign.WebIdKeyLoader;

import java.net.URI;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: ypanchenko Date: 28.07.2015
 */
public class WebIDVerificationAgent {
    final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private WebIdKeyLoader webIdKeyLoader;

    public WebIDVerificationAgent() {
    }

    @SuppressWarnings("unchecked")
    public boolean verify(PublicKey publicKey, URI webId) {
        if (publicKey instanceof ECPublicKey) {
            try {
                ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
                Set<PublicKey> keys = webIdKeyLoader.loadKey(webId.toString());
                for (PublicKey key : keys) {
                    if (isSameKey(ecPublicKey, key)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                throw new InternalAuthenticationServiceException("Could not verify key", e);
            }
        } else {
            throw new InternalAuthenticationServiceException(
                            "Key type " + publicKey.getAlgorithm() + " not supported");
        }
        return false;
    }

    public boolean isSameKey(ECPublicKey ecPublicKey, PublicKey key) {
        ECPublicKey ecPublicKeyFetched = (ECPublicKey) key;
        if (ecPublicKey.getW().getAffineX().equals(ecPublicKeyFetched.getW().getAffineX())) {
            if (ecPublicKey.getW().getAffineY().equals(ecPublicKeyFetched.getW().getAffineY())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return list of those webIDs that were successfully verified by fetching the
     * webID's url and comparing public key data found there with the provided in
     * constructor public key data
     */
    public List<String> verify(PublicKey publicKey, List<URI> webIDs) throws AuthenticationException {
        List<String> verified = new ArrayList<>();
        for (URI webID : webIDs) {
            if (verify(publicKey, webID)) {
                verified.add(webID.toString());
            }
        }
        return verified;
    }
}
