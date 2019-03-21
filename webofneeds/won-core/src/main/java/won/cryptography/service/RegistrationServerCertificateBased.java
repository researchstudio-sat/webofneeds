package won.cryptography.service;

import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.ssl.AliasFromFingerprintGenerator;
import won.cryptography.ssl.AliasGenerator;
import won.protocol.exception.WonProtocolException;
import won.protocol.service.ApplicationManagementService;

import javax.transaction.Transactional;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko Date: 08.10.2015
 */
public class RegistrationServerCertificateBased implements RegistrationServer {

  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ApplicationManagementService ownerManagementService;
  private TrustStrategy trustStrategy;
  private AliasGenerator aliasGenerator = new AliasFromFingerprintGenerator();

  public RegistrationServerCertificateBased(final TrustStrategy trustStrategy) {
    // this.trustStoreService = trustStoreService;
    this.trustStrategy = trustStrategy;
  }

  @Transactional
  public String registerOwner(Object certificateChainObj) throws WonProtocolException {
    String alias = null;
    X509Certificate[] ownerCertChain = new X509Certificate[] { (X509Certificate) certificateChainObj };
    checkTrusted(ownerCertChain);
    try {
      alias = aliasGenerator.generateAlias(ownerCertChain[0]);
      logger.info("Public key hash to be used as ownerApplicationId: {}", alias);
      alias = ownerManagementService.registerOwnerApplication(alias);
    } catch (Exception e) {
      logger.warn("could not register owner", e);
      throw new WonProtocolException(e);
    }
    return alias;
  }

  public String registerNode(Object certificateChainObj) throws WonProtocolException {

    X509Certificate[] nodeCertChain = new X509Certificate[] { (X509Certificate) certificateChainObj };
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

}
