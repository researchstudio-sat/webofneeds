package won.cryptography.service;

import org.apache.http.conn.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.x509.X500Name;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko
 * Date: 05.08.2015
 */
public class TOFUTrustStrategy implements TrustStrategy
{
  private TrustStoreService trustStoreService;

  private final Logger logger = LoggerFactory.getLogger(getClass());


  public void setTrustStoreService(TrustStoreService trustStoreService) {
    this.trustStoreService = trustStoreService;
  }


  public boolean isTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {

    // the host should be checked by other means. E.g.  apache https uses default HostNameVerifier that
    // verifies that the target hostname matches the names stored inside the server's X.509 certificate

    // extract certificate
    X509Certificate serverCert = x509Certificates[0];

    // this should not happen
    if (serverCert == null) {
      return false;
    }

    // consider trusted if we already know this certificate
    if (trustStoreService.isCertKnown(serverCert)) {
      logger.info("Server certificate is already known by client!");
      return true;
    }

    String alias = null;
    try {
      X500Name dnName = new X500Name(serverCert.getSubjectDN().getName());
      alias = dnName.getCommonName();
      if (alias == null || alias.isEmpty()) {
        throw new IOException("CN is null - cannot accept as alias");
      }
    } catch (IOException e) {
      logger.warn("Cannot create alias for the certificate", e);
      return false;
    }


    // add to trusted and return true if we don't know this certificate and don't know the domain name
    // mentioned in the certificate
    Certificate retrieved = trustStoreService.getCertificate(alias);
    if (retrieved == null) {
      // this is the first time this server contacts us, add its certificated to trusted ones
      trustStoreService.addCertificate(alias, serverCert);
      logger.info("Server certificate is add as TOFU and from now on is trusted by client!");
      return true;

    } else {
      logger.warn("Server is already known by client but with different certificate - no trust for new certificate " +
                    "added!");
      return false;
    }

  }

  /**
   * This will load the system default keystore, i.e. all generally accepted CA's certificates.
   * Not sure if we need this. But if we do, just initialize an SSLContext with the array of
   * TrustManagers that include this default java trust manager.
   * Or, apply this trust manager inside the custom trust manager as alternative to the custom
   * trust checks when those fails...
   * @return
   */
  private static X509TrustManager getDefaultTrustManager() throws NoSuchAlgorithmException, KeyStoreException {

    TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
    // initializing with null loads the system default keystore, will work only for the client
    tmf.init((KeyStore) null);
    for (TrustManager t : tmf.getTrustManagers()) {
      if (t instanceof X509TrustManager) {
        return (X509TrustManager)t;
      }
    }
    return null;
  }

}
