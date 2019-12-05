package won.cryptography.webid;

import java.net.URI;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * User: ypanchenko Date: 28.07.2015
 */
public class WebIDVerificationAgent {
    final Logger logger = LoggerFactory.getLogger(getClass());
    private LinkedDataSource linkedDataSource;
    private final Ehcache webIdCache;

    public WebIDVerificationAgent() {
        CacheManager manager = CacheManager.getInstance();
        this.webIdCache = new Cache("WebIDVerificationAgent", 100, false, false, 3600, 3600);
        manager.addCache(webIdCache);
    }

    @SuppressWarnings("unchecked")
    public boolean verify(PublicKey publicKey, URI webId) {
        Dataset dataset;
        // TODO for RSA key
        // if (publicKey instanceof RSAPublicKey) {
        // RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        // BigInteger modulus = WonRdfUtils.SignatureUtils.getRsaPublicKeyModulus(webID,
        // dataset);
        // BigInteger exponent =
        // WonRdfUtils.SignatureUtils.getRsaPublicKeyExponent(webID, dataset);
        // if (exponent != null && rsaPublicKey.getPublicExponent().equals(exponent)) {
        // if (modulus != null && rsaPublicKey.getModulus().equals(modulus)) {
        // verified.add(webID.toString());
        // }
        // }
        // }
        if (publicKey instanceof ECPublicKey) {
            ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
            WonKeysReaderWriter ecKeyReader = new WonKeysReaderWriter();
            boolean isCached = false;
            Set<PublicKey> keys;
            try {
                Element cachedElement = webIdCache.get(webId);
                if (cachedElement != null) {
                    keys = (Set<PublicKey>) cachedElement.getObjectValue();
                    isCached = true;
                } else {
                    dataset = linkedDataSource.getDataForResource(webId);
                    keys = ecKeyReader.readFromDataset(dataset, webId.toString());
                }
            } catch (Exception e) {
                throw new InternalAuthenticationServiceException("Could not verify key", e);
            }
            for (PublicKey key : keys) {
                if (isSameKey(ecPublicKey, key)) {
                    if (!isCached) {
                        webIdCache.put(new Element(webId, keys));
                    }
                    return true;
                }
            }
        } else {
            throw new InternalAuthenticationServiceException("Key type " + publicKey.getAlgorithm() + " not supported");
        }
        return false;
    }

    public boolean isSameKey(ECPublicKey ecPublicKey, PublicKey key) {
        ECPublicKey ecPublicKeyFetched = (ECPublicKey) key;
        // TODO check if equals work
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

    public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }
}
