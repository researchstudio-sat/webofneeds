package won.cryptography.webid;

import won.protocol.vocabulary.WONCRYPT;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by hfriedrich on 18.04.2016.
 */
public class CertificateUtils
{

  /**
   *  Extract client certificate chain from http request.
   *  check if won node is behind a reverse proxy => handle client certificate a different way.
   *  see http://techblog.bozho.net/tls-client-authentication/
   *
   * @param request http request
   * @param behindProxy true if running behind reverse proxy and client authentication is handled by proxy
   * @return extracted client certificate chain
   * @throws CertificateException
   * @throws UnsupportedEncodingException
   */
  public static X509Certificate[] extractClientCertificateFromRequest(final HttpServletRequest request, boolean behindProxy)
    throws CertificateException, UnsupportedEncodingException {

    X509Certificate[] certificateChainObj = null;

    if (behindProxy) {

      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      String certificateHeader = request.getHeader(WONCRYPT.CLIENT_CERTIFICATE_HEADER);

      if (certificateHeader == null) {
        throw new CertificateException(
          "No HTTP header 'X-Client-Certificate' set that contains client authentication certificate! If property " +
            "'client.authentication.behind.proxy' is set to true, this header must be " +
            "set by the reverse proxy!");
      }

      // the load balancer (e.g. nginx) forwards the certificate into a header by replacing new lines with whitespaces
      // (2 or more). Also replace tabs, which sometimes nginx may send instead of whitespace
      String certificateContent = certificateHeader.replaceAll("\\s{2,}", System.lineSeparator())
                                                   .replaceAll("\\t+", System.lineSeparator());
      X509Certificate[] userCertificate = new X509Certificate[1];
      userCertificate[0] = (X509Certificate) certificateFactory
        .generateCertificate(new ByteArrayInputStream(certificateContent.getBytes("ISO-8859-11")));
      certificateChainObj = userCertificate;

    } else {
      certificateChainObj = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
      if (certificateChainObj == null) {
        throw new CertificateException(
          "Client certificate attribute is null! Check if you are behind a proxy server that takes care about the " +
            "client authentication already. If so, set the property 'client.authentication.behind.proxy' to true and " +
            "make sure the proxy sets the HTTP header 'X-Client-Certificate' appropriately to the sent client certificate");
      }
    }

    return certificateChainObj;
  }

}
