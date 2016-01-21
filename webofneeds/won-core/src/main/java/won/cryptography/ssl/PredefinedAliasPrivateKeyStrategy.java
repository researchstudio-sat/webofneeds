package won.cryptography.ssl;

import org.apache.http.conn.ssl.PrivateKeyDetails;
import org.apache.http.conn.ssl.PrivateKeyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.Map;

/**
 * When the server requests a certificate, and if it does not specify which one it wants (e.g. if server has trusted
 * CA's or trusted certificates, it can specify CN of the cerificate when asking for the client certificate), the
 * client can send any certificate from its keystore. I.e. if we have multiple certificates in the keystore, and we
 * want to do a request with a specific one out of all of them, we should provide it when asked. It can be done by
 * using for each request this private key strategy, that would serve the certificate with specified alias from
 * the keystore, when asked.
 *
 * User: ypanchenko
 * Date: 27.07.2015
 */
public class PredefinedAliasPrivateKeyStrategy implements PrivateKeyStrategy
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private String alias;

  public PredefinedAliasPrivateKeyStrategy(String alias) {
    this.alias = alias;
  }

  public String chooseAlias(final Map<String, PrivateKeyDetails> map, final Socket socket) {
    logger.debug("Choosen alias: " + alias);
    return alias;
  }
}
