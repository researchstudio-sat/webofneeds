package won.cryptography.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.conn.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.WonProtocolException;
import won.protocol.service.ApplicationManagementService;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko
 * Date: 08.10.2015
 */
public class RegistrationServerCertificateBased implements RegistrationServer
{

  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ApplicationManagementService ownerManagementService;
  private TrustStrategy trustStrategy;


  public RegistrationServerCertificateBased(final TrustStrategy trustStrategy) {
    //this.trustStoreService = trustStoreService;
    this.trustStrategy = trustStrategy;
  }

  public String registerOwner(Object certificateChainObj) throws WonProtocolException {

    String ownerId = null;
    X509Certificate[] ownerCertChain = extractCertificateChain(certificateChainObj);
    checkTrusted(ownerCertChain);
    try {
      String ownerSha1Fingerprint = DigestUtils.shaHex(ownerCertChain[0].getEncoded());
      ownerId = ownerManagementService.registerOwnerApplication(ownerSha1Fingerprint);
    } catch (CertificateException e) {
      throw new WonProtocolException(e);
    }
    logger.info("Registered owner with id " + ownerId);
    return ownerId;
  }

  public String registerNode(Object certificateChainObj) throws WonProtocolException {

    X509Certificate[] nodeCertChain = extractCertificateChain(certificateChainObj);
    checkTrusted(nodeCertChain);
    return null;
  }

  private void checkTrusted(final X509Certificate[] ownerCertChain) throws WonProtocolException {
    try {
      if (!trustStrategy.isTrusted(ownerCertChain, "CLIENT_CERT")) {
        throw new WonProtocolException("Client cannot be trusted!");
      }
    } catch (CertificateException e) {
      new WonProtocolException(e);
    }
  }

  private X509Certificate extractCertificate(final Object certificateChainObj)  throws WonProtocolException {
    X509Certificate clientCert[] = extractCertificateChain(certificateChainObj);
    return clientCert[0];
  }

  private X509Certificate[] extractCertificateChain(final Object certificateChainObj)  throws WonProtocolException {
    // Registration without certificate is not supported:
    if (certificateChainObj == null) {
      String msg = "Cannot register - no credentials provided!";
      logger.info(msg);
      throw new WonProtocolException(msg);
    }

    // Registration without certificate is not supported:
    if ( !(certificateChainObj instanceof X509Certificate[]) || ((X509Certificate[]) certificateChainObj).length < 1) {
      String msg = "Cannot register - provided credentials do not contain X509 certificate";
      logger.info(msg);
      throw new WonProtocolException(msg);
    }

    // Prepare certificate and calculated from it owner-id:
    X509Certificate[] clientCert = ((X509Certificate[]) certificateChainObj);
    return clientCert;
  }
}
