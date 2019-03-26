package won.cryptography.webid;

import java.net.URI;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.cryptography.service.CertificateService;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * Trust all the certificates that contains at least one verified webID in
 * certificate's subject alternative names. Verified webID means that the WebID
 * URI is resolved and the public key fetched from there corresponds to the
 * public key of the presented certificate.
 *
 * User: ypanchenko Date: 23.10.2015
 */
public class TrustWebIdStrategy implements TrustStrategy {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private WebIDVerificationAgent verificationAgent;

  public TrustWebIdStrategy(LinkedDataSource linkedDataSource) {
    this.verificationAgent = new WebIDVerificationAgent();
    this.verificationAgent.setLinkedDataSource(linkedDataSource);

  }

  public boolean isTrusted(final X509Certificate[] x509Certificates, final String authType)
      throws CertificateException {

    if (x509Certificates == null || x509Certificates.length < 1) {
      return false;
    }

    // extract certificate and key
    X509Certificate cert = x509Certificates[0];
    PublicKey publicKey = cert.getPublicKey();

    // extract webID (can be several)
    List<URI> webIDs = null;
    try {
      webIDs = CertificateService.getWebIdFromSubjectAlternativeNames(cert);
    } catch (CertificateParsingException e) {
      logger.warn("error extracting WebIDs from subject alternative names", e);
      return false;
    }
    if (webIDs == null || webIDs.isEmpty()) {
      logger.warn("no WebIDs found in subject alternative names");
      return false;
    }

    // verify
    List<String> verified = null;
    try {
      verified = verificationAgent.verify(publicKey, webIDs);
    } catch (Exception e) {
      logger.warn("Error during WebIDs verification " + webIDs.toString());
      return false;
    }

    if (verified == null || verified.isEmpty()) {
      logger.warn("WebIDs do not pass verification " + webIDs.toString());
      return false;
    } else {
      return true;
    }

  }
}
