package won.cryptography.depr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko
 * Date: 08.10.2015
 */
public class WonConfigBasedTrustManagerWrapperDepr implements X509TrustManager
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private X509TrustManager trustManagerFromConfig;

//  public WonConfigBasedTrustManagerWrapperDepr(TransmissionConfig config, WonCommunicatingParty party) {
//    try {
//      switch (party) {
//        case MESSAGING_SERVER:
//          trustManagerFromConfig = config.getMessagingServerTrustManager();
//          break;
//        case MESSAGING_CLIENT:
//          trustManagerFromConfig = config.getMessagingClientTrustManager();
//          break;
//        default:
//          throw new IllegalArgumentException("TrustManager for " + party + " not supported");
//      }
//
//    } catch (Exception e) {
//      logger.error("Error initializing TrustManager for messaging for " + party, e);
//      throw new IllegalArgumentException(e);
//    }
//  }


  @Override
  public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
    trustManagerFromConfig.checkClientTrusted(x509Certificates, s);
  }

  @Override
  public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
    trustManagerFromConfig.checkServerTrusted(x509Certificates, s);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return trustManagerFromConfig.getAcceptedIssuers();
  }
}
