package won.cryptography.rdfsign;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.service.CryptographyService;
import won.cryptography.service.TrustStoreService;
import won.cryptography.service.keystore.KeyStoreService;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

public class WebIdKeyLoader {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Duration retryInterval = Duration.ofHours(1);
    private final Ehcache webIdCache;
    @Autowired
    private LinkedDataSource linkedDataSource;
    private WonKeysReaderWriter wonKeysReaderWriter = new WonKeysReaderWriter();
    @Autowired
    private KeyStoreService keyStoreService;
    @Autowired
    private TrustStoreService trustStoreService;
    @Autowired
    private CryptographyService cryptographyService;

    public WebIdKeyLoader() {
        CacheManager manager = CacheManager.getInstance();
        Cache cache = manager.getCache(this.getClass().getName());
        if (cache == null) {
            cache = new Cache(this.getClass().getName(), 1000, false, false, 3600, 3600);
            manager.addCache(cache);
        }
        this.webIdCache = cache;
    }

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    /**
     * Loads the key with the specified URI. Returns an empty set if none found.
     *
     * @param keyURI
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidKeySpecException
     */
    public Set<PublicKey> loadKey(String keyURI)
                    throws NoSuchAlgorithmException, NoSuchProviderException,
                    InvalidKeySpecException {
        Element cachedElement = webIdCache.get(keyURI);
        if (cachedElement != null) {
            if (cachedElement.getObjectValue() instanceof Set) {
                return (Set<PublicKey>) cachedElement.getObjectValue();
            } else if (cachedElement.getObjectValue() instanceof UnavailableKey) {
                if (!((UnavailableKey) cachedElement.getObjectValue()).isOlderThan(retryInterval)) {
                    return Collections.emptySet();
                }
            }
        }
        PublicKey key = keyStoreService.getPublicKey(keyURI);
        if (key != null) {
            logger.debug("found key {} in local key store", keyURI);
            webIdCache.put(new Element(keyURI, key));
            return Set.of(key);
        }
        Certificate cert = trustStoreService.getCertificate(keyURI);
        if (cert != null) {
            key = cert.getPublicKey();
            if (key != null) {
                logger.debug("found key {} in local trust store", keyURI);
                webIdCache.put(new Element(keyURI, key));
                return Set.of(key);
            }
        }
        Set<PublicKey> ret = loadKeyRemotely(keyURI);
        if (ret != null) {
            if (ret.size() > 0) {
                webIdCache.put(new Element(keyURI, ret));
            } else {
                webIdCache.put(new Element(keyURI, new UnavailableKey(URI.create(keyURI))));
            }
        }
        return ret;
    }

    public Set<PublicKey> loadKeyRemotely(String refKey)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        try {
            Dataset keyDataset = linkedDataSource.getDataForResource(
                            URI.create(refKey),
                            URI.create(cryptographyService.getDefaultPrivateKeyAlias()));
            Set<PublicKey> resolvedKeys = wonKeysReaderWriter.readFromDataset(keyDataset, refKey);
            return resolvedKeys;
        } catch (LinkedDataFetchingException e) {
            logger.info("Error fetching public key for uri {}: {}", refKey, e.getMessage());
            return Collections.emptySet();
        }
    }

    private static class UnavailableKey implements Serializable {
        private URI uri;
        private Instant timestamp;

        public UnavailableKey(URI uri) {
            this.uri = uri;
            this.timestamp = Instant.now();
        }

        public boolean isOlderThan(Duration duration) {
            return timestamp.isBefore(Instant.from(retryInterval.subtractFrom(Instant.now())));
        }

        public URI getUri() {
            return uri;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
