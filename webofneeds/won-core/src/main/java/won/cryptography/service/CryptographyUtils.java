package won.cryptography.service;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import won.cryptography.ssl.PredefinedAliasStrategy;

import javax.crypto.Cipher;
import javax.net.ssl.SSLContext;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * User: fsalcher Date: 12.06.2014
 */
public class CryptographyUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Ehcache ehcache;
    private static final Object monitor = new Object();
    private static final String keyStoreType = "UBER";
    static {
        CacheManager manager = CacheManager.getInstance();
        ehcache = new Cache("sslContextCache", 500, false, false, 3600, 600);
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

    private static SSLContext getSSLContext(final String privateKeyAlias, final KeyStore keyStore, final String ksPass,
                    final KeyStore trustStore, TrustStrategy trustStrategy,
                    boolean allowCached) throws Exception {
        if (allowCached) {
            return getCachedSslContextForKeystore(privateKeyAlias,
                            () -> {
                                try {
                                    return createSSLContextBuilder(privateKeyAlias, keyStore, ksPass, trustStore,
                                                    trustStrategy)
                                                                    .build();
                                } catch (Exception exception) {
                                    logger.error("Error creating ssl context", exception);
                                }
                                return null;
                            });
        } else {
            return createSSLContextBuilder(privateKeyAlias, keyStore, ksPass, trustStore, trustStrategy).build();
        }
    }

    private static String makeCacheKey(String privateKeyAlias) {
        return privateKeyAlias == null ? "no client cert" : privateKeyAlias;
    }

    private static SSLContext getCachedSslContextForKeystore(final String privateKeyAlias,
                    Supplier<SSLContext> sslContextSupplier) throws Exception {
        Instant start = Instant.now();
        String cacheKey = makeCacheKey(privateKeyAlias);
        logger.debug("Creating or obtaining cached SSL context for cache key {}", cacheKey);
        Element cacheElement = ehcache.get(cacheKey);
        SSLContext sslContext;
        if (cacheElement != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using cached SSL context");
            }
            return (SSLContext) cacheElement.getObjectValue();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("No cached SSL context found");
        }
        // we want to avoid creating the sslContext multiple times, so we snychronize on
        // an object shared by all threads:
        synchronized (monitor) {
            if (logger.isDebugEnabled()) {
                logger.debug("Inside critical section, {} millis since method start",
                                Duration.between(start, Instant.now()).toMillis());
            }
            // now we have to check again (maybe we're in the thread that had to wait - in
            // that case, the
            // sslContext has been created already
            cacheElement = ehcache.get(cacheKey);
            if (cacheElement == null) {
                sslContext = sslContextSupplier.get();
                cacheElement = new Element(cacheKey, sslContext);
                ehcache.put(cacheElement);
                if (logger.isDebugEnabled()) {
                    logger.debug("new SSL context created, {} millis since method start",
                                    Duration.between(start, Instant.now()).toMillis());
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("SSL context initialized in concurrent thread, using that one; {} millis since method start",
                                    Duration.between(start, Instant.now()).toMillis());
                }
            }
        }
        return (SSLContext) cacheElement.getObjectValue();
    }

    private static SSLContextBuilder createSSLContextBuilder(String privateKeyAlias, final KeyStore keyStore,
                    final String ksPass, final KeyStore trustStore, TrustStrategy trustStrategy)
                    throws Exception {
        Instant start = Instant.now();
        SSLContextBuilder contextBuilder = SSLContexts.custom();
        if (privateKeyAlias != null) {
            if (!keyStore.containsAlias(privateKeyAlias)) {
                throw new IllegalStateException(String.format(
                                "Cannot create SSL context for alias %s: no key with that alias found in keystore",
                                privateKeyAlias));
            }
            KeyStore ks = createKeystoreForSSLContext(privateKeyAlias, keyStore, ksPass);
            contextBuilder.loadKeyMaterial(ks, ksPass.toCharArray(),
                            new PredefinedAliasStrategy(privateKeyAlias));
        }
        // if trustStore is null, default CAs trust store is used
        contextBuilder.loadTrustMaterial(trustStore, trustStrategy);
        if (logger.isDebugEnabled()) {
            logger.debug("Loaded key material in {} millis", Duration.between(start, Instant.now()).toMillis());
        }
        return contextBuilder;
    }

    private static KeyStore createKeystoreForSSLContext(String privateKeyAlias, KeyStore keyStore, String ksPass) {
        try {
            KeyStore newKeyStore = null;
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating keystore for SSL context...");
                }
                newKeyStore = java.security.KeyStore.getInstance(keyStoreType, BCProvider.getInstance());
            } catch (Exception e) {
                // try again with standard provider resolution
                try {
                    newKeyStore = java.security.KeyStore.getInstance(keyStoreType);
                } catch (Exception e2) {
                    logger.error("Error initializing key store with provider {}: {} - fallback to default provider failed, too (see stacktrace below).",
                                    BCProvider.getInstance().getClass(), e.getMessage());
                    throw e2;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Created keystore: " + newKeyStore);
            }
            newKeyStore.load(null, null); // initialize the keystore
            newKeyStore.setEntry(privateKeyAlias,
                            keyStore.getEntry(privateKeyAlias, new KeyStore.PasswordProtection(ksPass.toCharArray())),
                            new KeyStore.PasswordProtection(ksPass.toCharArray()));
            if (logger.isDebugEnabled()) {
                logger.debug("Added key for specified alias {}", privateKeyAlias);
            }
            return newKeyStore;
        } catch (Exception e) {
            logger.error("Error initializing key store for SSL context with private key alias {}", privateKeyAlias);
            throw new IllegalStateException("Error initializing keystore for SSL context", e);
        }
    }

    public static RestTemplate createSslRestTemplate(final String privateKeyAlias, final KeyStore keyStore,
                    final String ksPass,
                    final KeyStore trustStore, TrustStrategy trustStrategy,
                    final Integer readTimeout, final Integer connectionTimeout, final boolean allowCached)
                    throws Exception {
        SSLContext sslContext = getSSLContext(privateKeyAlias, keyStore, ksPass, trustStore, trustStrategy,
                        allowCached);
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
}
