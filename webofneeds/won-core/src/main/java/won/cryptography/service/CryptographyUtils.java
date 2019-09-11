package won.cryptography.service;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import won.cryptography.ssl.PredefinedAliasPrivateKeyStrategy;

import javax.crypto.Cipher;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;

/**
 * User: fsalcher Date: 12.06.2014
 */
public class CryptographyUtils {
    public static boolean checkForUnlimitedSecurityPolicy() {
        try {
            int size = Cipher.getMaxAllowedKeyLength("RC5");
            System.out.println("max allowed key size: " + size);
            return size < 256;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static final Ehcache ehcache;
    static {
        CacheManager manager = CacheManager.getInstance();
        ehcache = new Cache("sslContextCache", 100, false, false, 3600, 600);
        manager.addCache(ehcache);
    }

    private static SSLContext getSSLContext(final KeyStore keyStore, final String ksPass,
                    final PrivateKeyStrategy keyStrategy, final KeyStore trustStore, TrustStrategy trustStrategy,
                    boolean allowCached) throws Exception {
        if (allowCached && keyStrategy instanceof PredefinedAliasPrivateKeyStrategy) {
            return getCachedSslContextForPredefinedAlias(keyStore, ksPass,
                            (PredefinedAliasPrivateKeyStrategy) keyStrategy, trustStore, trustStrategy);
        } else {
            return createSSLContextBuilder(keyStore, ksPass, keyStrategy, trustStore, trustStrategy).build();
        }
    }

    private static SSLContext getCachedSslContextForPredefinedAlias(final KeyStore keyStore, final String ksPass,
                    final PredefinedAliasPrivateKeyStrategy keyStrategy, final KeyStore trustStore,
                    final TrustStrategy trustStrategy) throws Exception {
        String cacheKey = keyStrategy.getAlias();
        Element cacheElement = ehcache.get(cacheKey);
        SSLContext sslContext = null;
        if (cacheElement == null) {
            // we want to avoid creating the sslContext multiple times, so we snychronize on
            // an object shared by all threads:
            synchronized (ehcache) {
                // now we have to check again (maybe we're in the thread that had to wait - in
                // that case, the
                // sslContext has been created already
                cacheElement = ehcache.get(cacheKey);
                if (cacheElement == null) {
                    sslContext = createSSLContextBuilder(keyStore, ksPass, keyStrategy, trustStore, trustStrategy)
                                    .build();
                    cacheElement = new Element(cacheKey, sslContext);
                    ehcache.put(cacheElement);
                }
            }
        }
        sslContext = (SSLContext) cacheElement.getObjectValue();
        return sslContext;
    }

    private static SSLContextBuilder createSSLContextBuilder(final KeyStore keyStore, final String ksPass,
                    final PrivateKeyStrategy keyStrategy, final KeyStore trustStore, TrustStrategy trustStrategy)
                    throws Exception {
        SSLContextBuilder contextBuilder = SSLContexts.custom();
        contextBuilder.loadKeyMaterial(keyStore, ksPass.toCharArray(), keyStrategy);
        // if trustStore is null, default CAs trust store is used
        contextBuilder.loadTrustMaterial(trustStore, trustStrategy);
        return contextBuilder;
    }

    public static RestTemplate createSslRestTemplate(final KeyStore keyStore, final String ksPass,
                    final PrivateKeyStrategy keyStrategy, final KeyStore trustStore, TrustStrategy trustStrategy,
                    final Integer readTimeout, final Integer connectionTimeout, final boolean allowCached)
                    throws Exception {
        SSLContext sslContext = getSSLContext(keyStore, ksPass, keyStrategy, trustStore, trustStrategy, allowCached);
        // here in the constructor, also hostname verifier, protocol version, cipher
        // suits, etc. can be specified
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);
        HttpClient httpClient = HttpClients.custom()// .useSystemProperties()
                        .setSSLSocketFactory(sslConnectionSocketFactory).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        if (readTimeout != null) {
            requestFactory.setReadTimeout(readTimeout);
        }
        if (connectionTimeout != null) {
            requestFactory.setConnectTimeout(connectionTimeout);
        }
        requestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
    }

    public static RestTemplate createSslRestTemplate(TrustStrategy trustStrategy, final Integer readTimeout,
                    final Integer connectionTimeout) throws Exception {
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, // if
                        // trustStore is null, default CAs trust store is used
                        trustStrategy).build();
        // here in the constructor, also hostname verifier, protocol version, cipher
        // suits, etc. can be specified
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);
        HttpClient httpClient = HttpClients.custom()// .useSystemProperties()
                        .setSSLSocketFactory(sslConnectionSocketFactory).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        if (readTimeout != null) {
            requestFactory.setReadTimeout(readTimeout);
        }
        if (connectionTimeout != null) {
            requestFactory.setConnectTimeout(connectionTimeout);
        }
        requestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
    }
}
