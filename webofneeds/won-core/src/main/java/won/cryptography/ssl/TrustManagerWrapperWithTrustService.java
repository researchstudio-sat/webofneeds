package won.cryptography.ssl;

import won.cryptography.service.TrustStoreService;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko Date: 13.08.2015
 */
public class TrustManagerWrapperWithTrustService implements X509TrustManager {

  private TrustStoreService trustStoreService;

  public TrustManagerWrapperWithTrustService(TrustStoreService trustStoreService) {
    this.trustStoreService = trustStoreService;
  }

  @Override
  public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType)
      throws CertificateException {
    X509TrustManager tm = null;
    try {
      tm = getDefaultTrustManagerForKeyStore(trustStoreService.getUnderlyingKeyStore());
    } catch (Exception e) {
      throw new RuntimeException("trust manager could not be initialized with dynamic key store", e);
    }
    if (tm == null) {
      throw new RuntimeException("default trust manager is not found");
    }
    tm.checkClientTrusted(x509Certificates, authType);
  }

  @Override
  public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType)
      throws CertificateException {
    X509TrustManager tm = null;
    try {
      tm = getDefaultTrustManagerForKeyStore(trustStoreService.getUnderlyingKeyStore());
    } catch (Exception e) {
      throw new RuntimeException("trust manager could not be initialized with dynamic key store", e);
    }
    if (tm == null) {
      throw new RuntimeException("default trust manager is not found");
    }
    tm.checkServerTrusted(x509Certificates, authType);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    X509TrustManager tm = null;
    try {
      tm = getDefaultTrustManagerForKeyStore(trustStoreService.getUnderlyingKeyStore());
    } catch (Exception e) {
      throw new RuntimeException("trust manager could not be initialized with dynamic key store", e);
    }
    if (tm == null) {
      throw new RuntimeException("default trust manager is not found");
    }
    return tm.getAcceptedIssuers();
  }

  private static X509TrustManager getDefaultTrustManagerForKeyStore(KeyStore keyStore)
      throws NoSuchAlgorithmException, KeyStoreException {

    TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
    // initializing with null loads the system default keystore, will work only for
    // the client
    tmf.init(keyStore);
    for (TrustManager t : tmf.getTrustManagers()) {
      if (t instanceof X509TrustManager) {
        return (X509TrustManager) t;
      }
    }
    return null;
  }

}
