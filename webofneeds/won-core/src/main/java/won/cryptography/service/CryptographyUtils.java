package won.cryptography.service;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.PrivateKeyStrategy;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
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


  public static RestTemplate createSslRestTemplate(final KeyStore keyStore, final String ksPass, final
  PrivateKeyStrategy keyStrategy, final KeyStore trustStore, TrustStrategy trustStrategy, final Integer readTimeout,
                                                   final Integer connectionTimeout)  throws Exception  {
    SSLContext sslContext = new SSLContextBuilder().loadKeyMaterial(keyStore,
                                                                    ksPass.toCharArray(),
                                                                    keyStrategy)
                                                   .loadTrustMaterial(trustStore, // if
                                                                      // trustStore is null, default CAs trust store is used
                                                                      trustStrategy)
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


}
