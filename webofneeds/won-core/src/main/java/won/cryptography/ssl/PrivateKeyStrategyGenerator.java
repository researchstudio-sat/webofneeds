package won.cryptography.ssl;

import org.apache.http.conn.ssl.PrivateKeyStrategy;

/**
 * User: ypanchenko
 * Date: 15.10.2015
 */
public class PrivateKeyStrategyGenerator
{
  public PrivateKeyStrategy createPrivateKeyStrategy(final String webID) {
    return new PredefinedAliasPrivateKeyStrategy(webID);
  }
}
