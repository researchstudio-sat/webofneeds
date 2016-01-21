package won.cryptography.ssl;

import org.apache.http.conn.ssl.PrivateKeyStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import won.cryptography.service.KeyStoreService;
import won.cryptography.service.TrustStoreService;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.X509KeyManager;

/**
 * User: ypanchenko
 * Date: 15.10.2015
 */
public class MessagingContext
{

  private X509KeyManager keyManager;
  private X509TrustManager trustManager;

  public MessagingContext() {
    // no key and trust managers initialized (null)
  }

  public MessagingContext(X509KeyManager keyManager, X509TrustManager trustManager) {
    this.keyManager = keyManager;
    this.trustManager = trustManager;
  }

  public MessagingContext(final KeyStoreService clientKeyStoreService,
                          final PrivateKeyStrategy clientDefaultAliasKeyStrategy,
                          final TrustStoreService trustStoreService) {
    keyManager = new KeyManagerWrapperWithKeyServiceAndStrategy(clientKeyStoreService, clientDefaultAliasKeyStrategy);
    trustManager = new TrustManagerWrapperWithTrustService(trustStoreService);
  }

  public MessagingContext(final KeyStoreService clientKeyStoreService,
                          final PrivateKeyStrategy clientDefaultAliasKeyStrategy,
                          final TrustStrategy trustStrategy) {
    keyManager = new KeyManagerWrapperWithKeyServiceAndStrategy(clientKeyStoreService, clientDefaultAliasKeyStrategy);
    trustManager = new TrustManagerWrapperWithStrategy(trustStrategy);
  }

  public X509KeyManager getClientKeyManager() throws Exception {
    return keyManager;
  }

  public TrustManager getClientTrustManager() throws Exception {
    return trustManager;
  }
}
