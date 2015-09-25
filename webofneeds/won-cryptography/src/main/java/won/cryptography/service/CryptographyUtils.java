package won.cryptography.service;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.PrivateKeyStrategy;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.net.ssl.*;
import java.security.KeyStore;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class CryptographyUtils {

    public static boolean checkForUnlimitedSecurityPolicy() {

        try {
            int size = Cipher.getMaxAllowedKeyLength("RC5");
            System.out.println("max allowed key size: " + size);
            return  size < 256;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Prepare a template with the SSL context with custom key and trust stores and with the TOFUTrustStrategy.
     *
     * @param keyStoreService
     * @param ksPass
     * @param trustStoreService
     * @return
     * @throws Exception
     */
    public static RestTemplate createSslTofuRestTemplate(KeyStoreService keyStoreService, String
      ksPass, TrustStoreService
      trustStoreService) throws Exception {
        return createSslTofuRestTemplate(keyStoreService, ksPass, trustStoreService, null, null);
    }


  public static RestTemplate createSslTofuRestTemplate(KeyStoreService keyStoreService, String
    ksPass, TrustStoreService trustStoreService, Integer readTimeout, Integer connectionTimeout) throws Exception {
    // make a call to register REST api in the SSL context with custom key and trust managers
    //TODO do it correctly with spring bean config, this can be helpful:
    //http://thespringway.info/spring-web/access-self-signed-ssl-certificate-with-resttemplate/
    TOFUTrustStrategy trustStrategy = new TOFUTrustStrategy();
    trustStrategy.setTrustStoreService(trustStoreService);
    PredefinedAliasPrivateKeyStrategy keyStrategy = new PredefinedAliasPrivateKeyStrategy(keyStoreService.getDefaultAlias());

    SSLContext sslContext = new SSLContextBuilder().loadKeyMaterial(keyStoreService.getUnderlyingKeyStore(),
                                                                    ksPass.toCharArray(), keyStrategy)
                                                   .loadTrustMaterial(null, trustStrategy)
                                                   .build();
    // here in the constructor, also hostname verifier, protocol version, cipher suits, etc. can be specified
    SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);

    HttpClient httpClient = HttpClients.custom()//.useSystemProperties()
      .setSSLSocketFactory(sslConnectionSocketFactory).build();
    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    if (readTimeout != null) {
      requestFactory.setReadTimeout(readTimeout.intValue());
    }
    if (connectionTimeout != null) {
      requestFactory.setConnectTimeout(connectionTimeout.intValue());
    }
    requestFactory.setHttpClient(httpClient);

    RestTemplate restTemplate = new RestTemplate(requestFactory);

    return restTemplate;
  }

    //TODO 1) make trust manager wrapper that only trusts certificate with brokerURI's (nodeURI's or its host)
    // from the trust store, e.g. TrustManagerWrapperWithStrategy - then it cannot be reused for connections to
    // different brokers/nodes
  // 2) return TrustManagerWrapperWithTrustService here - then it can be reused for connections to different nodes
  // (this one would internally create new x509trustmanager based on dynamically retrieved key/trust-store)
    public static TrustManager initializeTrustManager(TrustStoreService trustStoreService, String trustedAlias) throws
      Exception {
        KeyStore trustStore = trustStoreService.getUnderlyingKeyStore();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        //TODO what about password???
        tmf.init(trustStore);
        TrustManager tm = tmf.getTrustManagers()[0];
        return tm;
    }

    /**
     * Initialize KeyManager based on the key store of the key store service with the private key selection
     * strategy being the key with provided alias.
     *
     * @param keyStoreService
     * @param ksPass
     * @return
     * @throws Exception
     */
    public static KeyManager initializeKeyManager(KeyStoreService keyStoreService, String ksPass, String alias) throws
      Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        //TODO the right way to handle password?
        kmf.init(keyStoreService.getUnderlyingKeyStore(), ksPass.toCharArray());
        // TODO instead of this cast, iterate and select instance of X509KeyManager
        X509KeyManager km = (X509KeyManager) kmf.getKeyManagers()[0];
        // default alias of this key store should be this node's web-id
        km = new KeyManagerWrapperWithStrategy(km, new PredefinedAliasPrivateKeyStrategy(alias));
        return km;
    }

  private static HttpClient createHttpClientWithSslContext(
    final KeyStore requesterKeyStore, final String keyStorePass, final String requesterAlias, final KeyStore
    requesterTrustStore)
    throws Exception {

    PrivateKeyStrategy keyStrategy = new PredefinedAliasPrivateKeyStrategy(requesterAlias);

    SSLContext sslContext = null;

    if (requesterKeyStore == null) { // in this case the requester certificate cannot be provided to the server
      sslContext = new SSLContextBuilder()
        .loadTrustMaterial
          (requesterTrustStore)
          //(null, new TrustSelfSignedStrategy())
        .build();

    } else { // in this case the requester certificate will be returned to the server, if asked by the server
      sslContext = new SSLContextBuilder().loadKeyMaterial(requesterKeyStore, keyStorePass.toCharArray(), keyStrategy)
        .loadTrustMaterial
          (requesterTrustStore)
          //(null, new TrustSelfSignedStrategy())
        .build();
    }

    SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);

    HttpClient httpClient = HttpClients.custom().useSystemProperties().setSSLSocketFactory
      (sslConnectionSocketFactory).build();

    return httpClient;
  }

  public static RestTemplate createRestTemplateWithSslContext(
    final KeyStore requesterKeyStore, final String keyStorePass, final String requesterAlias, final KeyStore
    requesterTrustStore)
    throws Exception {



    HttpClient httpClient = CryptographyUtils.createHttpClientWithSslContext(requesterKeyStore,
                                                                             keyStorePass,
                                                                             requesterAlias,
                                                                             requesterTrustStore);
    HttpComponentsClientHttpRequestFactory requestFactory =
      new HttpComponentsClientHttpRequestFactory();
    requestFactory.setHttpClient(httpClient);


    RestTemplate restTemplate = new RestTemplate(requestFactory);

    return restTemplate;

  }

}
