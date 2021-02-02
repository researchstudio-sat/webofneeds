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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.net.ssl.SSLContext;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.function.Supplier;

/**
 * User: fsalcher Date: 12.06.2014
 */
public class CryptographyUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Ehcache ehcache;
    static {
        CacheManager manager = CacheManager.getInstance();
        ehcache = new Cache("sslContextCache", 100, false, false, 3600, 600);
        manager.addCache(ehcache);
    }

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

    private static SSLContext getSSLContext(final KeyStore keyStore, final String ksPass,
                    final PrivateKeyStrategy keyStrategy, final KeyStore trustStore, TrustStrategy trustStrategy,
                    boolean allowCached) throws Exception {
        if (allowCached) {
            return getCachedSslContextForKeystore(keyStore,
                            makeCacheKey(keyStore, keyStrategy, trustStore, trustStrategy), () -> {
                                try {
                                    return createSSLContextBuilder(keyStore, ksPass, keyStrategy, trustStore,
                                                    trustStrategy)
                                                                    .build();
                                } catch (Exception exception) {
                                    logger.error("Error creating ssl context", exception);
                                }
                                return null;
                            });
        } else {
            return createSSLContextBuilder(keyStore, ksPass, keyStrategy, trustStore, trustStrategy).build();
        }
    }

    private static String makeCacheKey(KeyStore keyStore,
                    PrivateKeyStrategy keyStrategy, KeyStore trustStore, TrustStrategy trustStrategy) {
        return keyStore.hashCode()
                        + "-" + keyStrategy.hashCode()
                        + "-" + trustStore.hashCode()
                        + "-" + trustStrategy.hashCode();
    }

    private static SSLContext getCachedSslContextForKeystore(final KeyStore keyStore, String cacheKey,
                    Supplier<SSLContext> sslContextSupplier) throws Exception {
        Element cacheElement = ehcache.get(cacheKey);
        SSLContext sslContext;
        if (cacheElement != null) {
            // check the size of the keystore - if it has changed, reload
            if (keyStoreHasChanged(keyStore, cacheElement)) {
                ehcache.remove(cacheElement);
                cacheElement = null;
            }
        }
        if (cacheElement == null) {
            // we want to avoid creating the sslContext multiple times, so we snychronize on
            // an object shared by all threads:
            synchronized (ehcache) {
                // now we have to check again (maybe we're in the thread that had to wait - in
                // that case, the
                // sslContext has been created already
                cacheElement = ehcache.get(cacheKey);
                if (cacheElement == null || keyStoreHasChanged(keyStore, cacheElement)) {
                    sslContext = sslContextSupplier.get();
                    cacheElement = new Element(cacheKey, new CacheEntry(sslContext, keyStore.size()));
                    ehcache.put(cacheElement);
                }
            }
        }
        return ((CacheEntry) cacheElement.getObjectValue()).getSslContext();
    }

    private static boolean keyStoreHasChanged(KeyStore keyStore, Element cacheElement) throws KeyStoreException {
        return ((CacheEntry) cacheElement.getObjectValue()).keystoreSize != keyStore.size();
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
        return new RestTemplate(requestFactory);
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
        return new RestTemplate(requestFactory);
    }

    private static class CacheEntry {
        private SSLContext sslContext;
        private int keystoreSize;

        public CacheEntry(SSLContext sslContext, int keystoreSize) {
            this.sslContext = sslContext;
            this.keystoreSize = keystoreSize;
        }

        public SSLContext getSslContext() {
            return sslContext;
        }

        public int getKeystoreSize() {
            return keystoreSize;
        }
    }
}
