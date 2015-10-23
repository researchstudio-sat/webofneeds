package won.cryptography.depr;

import org.apache.http.conn.ssl.PrivateKeyStrategy;
import won.cryptography.service.KeyStoreService;
import won.cryptography.ssl.TrustManagerWrapperWithTrustService;
import won.cryptography.service.TrustStoreService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;

/**
 * User: ypanchenko
 * Date: 15.10.2015
 */
public class MessagingContextDepr
{


  private KeyStoreService keyStoreService;
  private PrivateKeyStrategy privateKeyStrategy;
  private TrustStoreService trustStoreService;

  //TODO setters
  //TODO messaging context in case no ssl can return null km/tm and in that case the calling mathod should deal with
  // it...
  public MessagingContextDepr(KeyStoreService keyStoreService, PrivateKeyStrategy privateKeyStrategy, TrustStoreService
    trustStoreService) {
    this.keyStoreService = keyStoreService;
    this.privateKeyStrategy = privateKeyStrategy;
    this.trustStoreService = trustStoreService;
  }

  public X509KeyManager getClientKeyManager() throws Exception {
    return getStoreDefaultAliasKeyManager(keyStoreService);
  }

  // TODO excpetion? null?
  // TODO can reuse or should be created per request?
  public TrustManager getClientTrustManager() throws Exception {
    TrustManagerWrapperWithTrustService tm = new TrustManagerWrapperWithTrustService(trustStoreService);
    return tm;
  }


//  public X509KeyManager getMessagingServerKeyManager() throws Exception {
//    return getStoreDefaultAliasKeyManager(serverKeyStoreService);
//  }

  private X509KeyManager getStoreDefaultAliasKeyManager(KeyStoreService ksService) throws Exception {
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(ksService.getUnderlyingKeyStore(), ksService.getPassword().toCharArray());
    // TODO instead of this cast, iterate and select instance of X509KeyManager
    X509KeyManager km = (X509KeyManager) kmf.getKeyManagers()[0];
    // default alias of this key store should be this node's web-id
    //new PredefinedAliasPrivateKeyStrategy(ksService.getDefaultAlias())
    km = new KeyManagerWrapperWithStrategyDepr(km, this.privateKeyStrategy);
    return km;
  }
  //TrustManager tm = securityConfig.getMessagingClientTrustManager();

}

