package won.protocol.util;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.io.OutputStream;

public class SSLRC4Poke {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + SSLRC4Poke.class.getName() + " <host> <port> enable");
            System.exit(1);
        }
        try {
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(args[0], Integer.parseInt(args[1]));
            // String []cyphers = sslsocketfactory.getSupportedCipherSuites();
            if (args.length == 3) {
                sslsocket.setEnabledCipherSuites(
                                new String[] { "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5", "SSL_DH_anon_WITH_RC4_128_MD5",
                                                "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_WITH_RC4_128_MD5",
                                                "SSL_RSA_WITH_RC4_128_SHA", "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
                                                "TLS_ECDHE_RSA_WITH_RC4_128_SHA", "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
                                                "TLS_ECDH_RSA_WITH_RC4_128_SHA", "TLS_ECDH_anon_WITH_RC4_128_SHA",
                                                "TLS_KRB5_EXPORT_WITH_RC4_40_MD5", "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
                                                "TLS_KRB5_WITH_RC4_128_MD5", "TLS_KRB5_WITH_RC4_128_SHA" });
            }
            InputStream in = sslsocket.getInputStream();
            OutputStream out = sslsocket.getOutputStream();
            // Write a test byte to get a reaction :)
            out.write(1);
            while (in.available() > 0) {
                System.out.print(in.read());
            }
            System.out.println("Successfully connected");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
