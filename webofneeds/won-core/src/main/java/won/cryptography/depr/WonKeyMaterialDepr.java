package won.cryptography.depr;

import org.apache.http.conn.ssl.PrivateKeyStrategy;
import won.cryptography.service.KeyStoreService;
import won.cryptography.ssl.PredefinedAliasPrivateKeyStrategy;

import java.security.KeyStore;

/**
 * User: ypanchenko
 * Date: 28.09.2015
 */
public class WonKeyMaterialDepr
{

  private KeyStoreService keyStoreService;

  public void setKeyStoreService(final KeyStoreService keyStoreService) {
    this.keyStoreService = keyStoreService;
  }

  public KeyStore getKeyStore() {
    return this.keyStoreService.getUnderlyingKeyStore();
  }

  public String getStorePass() {
    return this.keyStoreService.getPassword();
  }

  // default alias will be used...
  public PrivateKeyStrategy getPrivateKeyStrategy() {
    return new PredefinedAliasPrivateKeyStrategy(this.keyStoreService.getDefaultAlias());
  }

  public PrivateKeyStrategy getPrivateKeyStrategy(String alias) {
    return new PredefinedAliasPrivateKeyStrategy(alias);
  }
}
