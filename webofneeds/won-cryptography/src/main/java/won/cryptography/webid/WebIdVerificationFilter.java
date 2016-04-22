package won.cryptography.webid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.service.CertificateService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * User: ypanchenko
 * Date: 27.07.2015
 */
public class WebIdVerificationFilter implements Filter
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private AccessControlRules acl;
  @Autowired
  private WebIDVerificationAgent verificationAgent;

  // true if the node is behind a reverse proxy
  private boolean behindProxy;


  public boolean isBehindProxy() {
    return behindProxy;
  }

  public void setBehindProxy(final boolean behindProxy) {
    this.behindProxy = behindProxy;
  }


  public void init(final FilterConfig filterConfig) throws ServletException {

  }

  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
    throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpServletResponse httpResponse = (HttpServletResponse) response;

    X509Certificate[] certChain = null;
    try {
      certChain = CertificateUtils.extractClientCertificateFromRequest(httpRequest, behindProxy);
    } catch (CertificateException e) {
      logger.error(e.getMessage());
    }

    if (isCertificatePresented(httpRequest, httpResponse, certChain) && isWebIDAcessGranted(
      httpRequest, httpResponse, certChain[0])) {
      filterChain.doFilter(request, response);
    }
  }

  private boolean isWebIDAcessGranted(final HttpServletRequest request, final HttpServletResponse response, final
  X509Certificate cert) throws IOException {

    //extract alternative names (they are webID uris) from certificate
    List<URI> webIDs = null;
    try {
      webIDs = CertificateService.getWebIdFromSubjectAlternativeNames(cert);
    } catch (CertificateParsingException e) {
      logger.error("error extracting subject alternative names", e);
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "No cerificate provided for WebID protected resource");
      return false;
    }
    if (webIDs == null || webIDs.isEmpty()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "No WebID in subject alternative names specified");
      return false;
    }

    //extract public key from the certificate
    PublicKey publicKey = cert.getPublicKey();

    //Fetch that WebID URIs, extract public key from the fetched content
    //compare public key from content with public key from certificate
    List<String> verifiedWebIDs = null;

    try {
      verifiedWebIDs = verificationAgent.verify(publicKey, webIDs);
    } catch (Exception e) {
      logger.error("Error during WebID verification", e);
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Error during WebID verification");
      return false;
    }


    // check access control rules to find out which WebIDs (URIs) are allowed, and if
    // any of those correspond to the verified WebID from the certificate, this functionality is called GUARD
    // in WebID. If /no one of them match - reject.
    // E.g. for the webofneeds we can check, that this resource is in the event respository and extract its
    // sender/receiverNode/Need uris. If the WebID in the certificate corresponds to any of them - access is
    // allowed.

    // For this example we can just grand access to the current resource based on the predefined map, e.g.
    // a resource can be accessed by its resource public version WebID (e.g. .../public/1 can access .../private/1)
    boolean accessGranted = acl.isAccessPermitted(request.getRequestURL().toString(), verifiedWebIDs);

    //if they don't match - reject, if they do - grant access
    if (accessGranted) {
      return true;
    } else {
      logger.warn("Access not granted to requester with WebID {}", verifiedWebIDs.toString());
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access not granted to requester with WebID " +
        verifiedWebIDs.toString());
      return false;
    }

  }

  private boolean isCertificatePresented(final HttpServletRequest request, final HttpServletResponse response, final
  X509Certificate[] certChain) throws IOException {
    boolean presented = false;
    if (certChain != null && certChain.length > 0) {
      logger.debug("" + certChain[0].toString());
      presented = true;
    } else {
      logger.warn("No cerificate provided! Access denied");
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "No cerificate provided for WebID protected resource");
    }
    return presented;
  }

  public void destroy() {

  }
}
