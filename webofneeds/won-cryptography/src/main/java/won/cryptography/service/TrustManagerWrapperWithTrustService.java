package won.cryptography.service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko
 * Date: 13.08.2015
 */
public class TrustManagerWrapperWithTrustService implements X509TrustManager
{


  private TrustStoreService trustStoreService;


//  public TrustManagerWrapperWithTrustService(TrustStoreService trustStoreService) throws KeyStoreException,
//    NoSuchAlgorithmException, IOException, CertificateException {
//    this.trustStoreService = trustStoreService;
//    //this is just to check for default trust manager initialization exceptions before we start really using it
//    X509TrustManager tm = getDefaultTrustManagerForKeyStore(trustStoreService.getUnderlyingKeyStore());
//    //TODO a good way to trust my own certificate? necessary for broker-client inside same node communication
////TODO get from config file
//    //String defaultKeyStorePath = System.getProperty("javax.net.ssl.keyStore");
//    String defaultKeyStorePath = "C:/DATA/ypanchenko/apache-tomcat/apache-tomcat-8.0.24/conf/ssl/t-keystore.jks";
//    //String defaultKeyStorePass = System.getProperty("javax.net.ssl.keyStorePassword");
//    String defaultKeyStorePass = "changeit";
//    KeyStore ksKeys = KeyStore.getInstance("JKS");
//    ksKeys.load(new FileInputStream(defaultKeyStorePath), defaultKeyStorePass.toCharArray());
//    Certificate cert = ksKeys.getCertificateChain("1")[0];
//    trustStoreService.addCertificate("localhost-node-self-cert", cert);
//  }

  public TrustManagerWrapperWithTrustService(TrustStoreService trustStoreService) throws KeyStoreException,
    NoSuchAlgorithmException, IOException, CertificateException {
    this.trustStoreService = trustStoreService;
    //this is just to check for default trust manager initialization exceptions before we start really using it
    X509TrustManager tm = getDefaultTrustManagerForKeyStore(trustStoreService.getUnderlyingKeyStore());
  }

  // TODO find a better way to trust own certificate
  public TrustManagerWrapperWithTrustService(TrustStoreService trustStoreService, String ownKeyStorePath, String
    ownKeyStorePass, String ownKeyStoreAlias) throws KeyStoreException,
    NoSuchAlgorithmException, IOException, CertificateException {
    this.trustStoreService = trustStoreService;
    //this is just to check for default trust manager initialization exceptions before we start really using it
    X509TrustManager tm = getDefaultTrustManagerForKeyStore(trustStoreService.getUnderlyingKeyStore());
    //TODO a good way to trust my own certificate? necessary for broker-client inside same node communication
//TODO get from config file
    //String defaultKeyStorePath = System.getProperty("javax.net.ssl.keyStore");
    String defaultKeyStorePath = ownKeyStorePath;
    //String defaultKeyStorePass = System.getProperty("javax.net.ssl.keyStorePassword");
    String defaultKeyStorePass = ownKeyStorePass;
    KeyStore ksKeys = KeyStore.getInstance("JKS");
    ksKeys.load(new FileInputStream(defaultKeyStorePath), defaultKeyStorePass.toCharArray());
    Certificate cert = ksKeys.getCertificateChain(ownKeyStoreAlias)[0];
    trustStoreService.addCertificate("localhost-node-self-cert", cert);
  }

  @Override
  public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType) throws
    CertificateException {
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
  public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType) throws CertificateException {
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

  private static X509TrustManager getDefaultTrustManagerForKeyStore(KeyStore keyStore) throws NoSuchAlgorithmException,
    KeyStoreException {

    TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
    // initializing with null loads the system default keystore, will work only for the client
    tmf.init(keyStore);
    for (TrustManager t : tmf.getTrustManagers()) {
      if (t instanceof X509TrustManager) {
        return (X509TrustManager)t;
      }
    }
    return null;
  }

}
