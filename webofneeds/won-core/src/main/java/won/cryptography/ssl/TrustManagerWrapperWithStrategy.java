package won.cryptography.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.ssl.TrustStrategy;

/**
 * User: ypanchenko Date: 13.08.2015
 */
public class TrustManagerWrapperWithStrategy implements X509TrustManager {
    private TrustStrategy trustStrategy;

    public TrustManagerWrapperWithStrategy(TrustStrategy trustStrategy) {
        this.trustStrategy = trustStrategy;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType)
                    throws CertificateException {
        X509TrustManager tm = null;
        try {
            tm = getDefaultTrustManager();
        } catch (Exception e) {
            throw new RuntimeException("trust manager could not be initialized", e);
        }
        if (tm != null) {
            try {
                tm.checkClientTrusted(x509Certificates, authType);
            } catch (CertificateException ex) {
                if (!trustStrategy.isTrusted(x509Certificates, authType)) {
                    throw new CertificateException(
                                    "Client is not trusted neither by strategy nor by default trust manager");
                }
            }
        }
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType)
                    throws CertificateException {
        X509TrustManager tm = null;
        try {
            tm = getDefaultTrustManager();
        } catch (Exception e) {
            throw new RuntimeException("trust manager could not be initialized", e);
        }
        if (tm != null) {
            try {
                tm.checkServerTrusted(x509Certificates, authType);
            } catch (CertificateException ex) {
                if (!trustStrategy.isTrusted(x509Certificates, authType)) {
                    throw new CertificateException(
                                    "Server is not trusted neither by strategy nor by default trust manager");
                }
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509TrustManager tm = null;
        try {
            tm = getDefaultTrustManager();
        } catch (Exception e) {
            throw new RuntimeException("trust manager could not be initialized with dynamic key store", e);
        }
        if (tm == null) {
            throw new RuntimeException("default trust manager is not found");
        }
        return tm.getAcceptedIssuers();
    }

    private static X509TrustManager getDefaultTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        // initializing with null loads the system default keystore, will work only for
        // the client
        KeyStore ts = null;
        tmf.init(ts);
        for (TrustManager t : tmf.getTrustManagers()) {
            if (t instanceof X509TrustManager) {
                return (X509TrustManager) t;
            }
        }
        return null;
    }
}
