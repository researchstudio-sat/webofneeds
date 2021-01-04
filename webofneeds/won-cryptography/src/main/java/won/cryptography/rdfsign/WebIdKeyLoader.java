package won.cryptography.rdfsign;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.util.linkeddata.LinkedDataSource;

public class WebIdKeyLoader {
    private final Ehcache webIdCache;
    @Autowired
    private LinkedDataSource linkedDataSource;
    private WonKeysReaderWriter wonKeysReaderWriter = new WonKeysReaderWriter();

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    public WebIdKeyLoader() {
        CacheManager manager = CacheManager.getInstance();
        Cache cache = manager.getCache(this.getClass().getName());
        if (cache == null) {
            cache = new Cache(this.getClass().getName(), 1000, false, false, 3600, 3600);
            manager.addCache(cache);
        }
        this.webIdCache = cache;
    }

    public Set<PublicKey> loadKey(String keyURI)
                    throws NoSuchAlgorithmException, NoSuchProviderException,
                    InvalidKeySpecException {
        Element cachedElement = webIdCache.get(keyURI);
        if (cachedElement != null) {
            return (Set<PublicKey>) cachedElement.getObjectValue();
        } else {
            Set<PublicKey> ret = loadKeyRemotely(keyURI);
            if (ret != null && ret.size() > 0) {
                webIdCache.put(new Element(keyURI, ret));
            }
            return ret;
        }
    }

    public Set<PublicKey> loadKeyRemotely(String refKey)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Dataset keyDataset = linkedDataSource.getDataForResource(URI.create(refKey));
        // TODO replace the WonKeysReaderWriter methods with WonRDFUtils methods and use
        // the WonKeysReaderWriter
        // itself internally there in those methods
        Set<PublicKey> resolvedKeys = wonKeysReaderWriter.readFromDataset(keyDataset, refKey);
        return resolvedKeys;
    }
}
