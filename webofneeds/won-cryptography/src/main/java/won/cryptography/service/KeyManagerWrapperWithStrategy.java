package won.cryptography.service;

import org.apache.http.conn.ssl.PrivateKeyDetails;
import org.apache.http.conn.ssl.PrivateKeyStrategy;

import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 12.08.2015
 *
 * This class is a copy implementation of class TrustManagerDelegate of org.apache.http.conn.ssl.SSLContextBuilder
 * https://hc.apache.org/httpcomponents-client-4.4.x/httpclient/xref/org/apache/http/conn/ssl/SSLContextBuilder.html
 * Unfortunately, they don't provide it as public class. It is useful when the default implementation of X509KeyManager
 * is used but additionally the strategy of how to choose the key when the key store contains many keys is applied.
 */
public class KeyManagerWrapperWithStrategy implements X509KeyManager
{
  private final X509KeyManager keyManager;
  private final PrivateKeyStrategy aliasStrategy;

  public KeyManagerWrapperWithStrategy(final X509KeyManager keyManager, final PrivateKeyStrategy aliasStrategy) {
    super();
    this.keyManager = keyManager;
    this.aliasStrategy = aliasStrategy;
  }

  @Override
  public String[] getClientAliases(final String keyType, final Principal[] issuers) {
    return this.keyManager.getClientAliases(keyType, issuers);
  }

  @Override
  public String chooseClientAlias(final String[] keyTypes, final Principal[] issuers, final Socket socket) {
    final Map<String, PrivateKeyDetails> validAliases = new HashMap<String, PrivateKeyDetails>();
    for (final String keyType: keyTypes) {
      final String[] aliases = this.keyManager.getClientAliases(keyType, issuers);
      if (aliases != null) {
        for (final String alias: aliases) {
          validAliases.put(alias, new PrivateKeyDetails(keyType, this.keyManager.getCertificateChain(alias)));
        }
      }
    }
    return this.aliasStrategy.chooseAlias(validAliases, socket);
  }

  @Override
  public String[] getServerAliases(final String keyType, final Principal[] issuers) {
    return this.keyManager.getServerAliases(keyType, issuers);
  }

  @Override
  public String chooseServerAlias(final String keyType, final Principal[] issuers, final Socket socket) {
    final Map<String, PrivateKeyDetails> validAliases = new HashMap<String, PrivateKeyDetails>();
    final String[] aliases = this.keyManager.getServerAliases(keyType, issuers);
    if (aliases != null) {
      for (final String alias: aliases) {
        validAliases.put(alias, new PrivateKeyDetails(keyType, this.keyManager.getCertificateChain(alias)));
      }
    }
    return this.aliasStrategy.chooseAlias(validAliases, socket);
  }

  @Override
  public X509Certificate[] getCertificateChain(final String alias) {
    return this.keyManager.getCertificateChain(alias);
  }

  @Override
  public PrivateKey getPrivateKey(final String alias) {
    return this.keyManager.getPrivateKey(alias);
  }
}
