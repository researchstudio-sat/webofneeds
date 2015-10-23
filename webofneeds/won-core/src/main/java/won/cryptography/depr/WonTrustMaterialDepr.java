package won.cryptography.depr;

import org.apache.http.conn.ssl.TrustStrategy;

import java.security.KeyStore;

/**
 * User: ypanchenko
 * Date: 28.09.2015
 */
public class WonTrustMaterialDepr
{

  private TrustStrategy trustStrategy;
  private KeyStore trustStore;


  public KeyStore getTrustStore() {
    // null, when used in sslcontextbuilder will result in using the default CAs
    return trustStore;
  }

  public TrustStrategy getTrustStrategy() {
    return this.trustStrategy;
  }

  public void setTrustStrategy(final TrustStrategy trustStrategy) {
    this.trustStrategy = trustStrategy;
  }

  public void setTrustStore(final KeyStore trustStore) {
    this.trustStore = trustStore;
  }
}
