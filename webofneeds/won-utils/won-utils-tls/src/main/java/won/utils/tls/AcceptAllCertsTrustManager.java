package won.utils.tls;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * Trust manager that accepts any certificate.
 * 
 * Intended for use in a Web server that allows clients to present self signed
 * client certificates. Accepting any client certificate defers the decision
 * about access to a later stage in the request processing chain, i.e. to after
 * the TLS handshake. Thus, the server's access control layer or business logic
 * can make decisions based on the client certificate, which, in this case is
 * just an unforgeable, self-generated unique identifier for the client.
 * 
 */

public class AcceptAllCertsTrustManager implements X509TrustManager {

  public AcceptAllCertsTrustManager() {
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    // throw no exception - i.e. trust any certificate
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    // throw no exception - i.e. trust any certificate
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[] {};
  }

}
