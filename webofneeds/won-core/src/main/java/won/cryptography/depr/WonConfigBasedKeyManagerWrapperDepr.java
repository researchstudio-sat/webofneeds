package won.cryptography.depr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko
 * Date: 08.10.2015
 */
//TODO if works for server, should be changed to use messagingcontext instead of wonconfig
public class WonConfigBasedKeyManagerWrapperDepr implements X509KeyManager
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private X509KeyManager keyManagerFromConfig;

//  public WonConfigBasedKeyManagerWrapper(WonTlsSecurityConfig config, WonCommunicatingParty party) {
//    try {
//      switch (party) {
//        case MESSAGING_SERVER:
//          keyManagerFromConfig = config.getMessagingServerKeyManager();
//          break;
//        case MESSAGING_CLIENT:
//          keyManagerFromConfig = config.getMessagingClientKeyManager();
//          break;
//        default:
//          throw new IllegalArgumentException("KeyManager for " + party + " not supported");
//      }
//
//    } catch (Exception e) {
//      logger.error("Error initializing KeyManager for messaging for " + party, e);
//      throw new IllegalArgumentException(e);
//    }
//  }


  @Override
  public String[] getClientAliases(final String s, final Principal[] principals) {
    return keyManagerFromConfig.getClientAliases(s, principals);
  }

  @Override
  public String chooseClientAlias(final String[] strings, final Principal[] principals, final Socket socket) {
    return keyManagerFromConfig.chooseClientAlias(strings, principals, socket);
  }

  @Override
  public String[] getServerAliases(final String s, final Principal[] principals) {
    return keyManagerFromConfig.getServerAliases(s, principals);
  }

  @Override
  public String chooseServerAlias(final String s, final Principal[] principals, final Socket socket) {
    return keyManagerFromConfig.chooseServerAlias(s, principals, socket);
  }

  @Override
  public X509Certificate[] getCertificateChain(final String s) {
    return keyManagerFromConfig.getCertificateChain(s);
  }

  @Override
  public PrivateKey getPrivateKey(final String s) {
    return keyManagerFromConfig.getPrivateKey(s);
  }
}
