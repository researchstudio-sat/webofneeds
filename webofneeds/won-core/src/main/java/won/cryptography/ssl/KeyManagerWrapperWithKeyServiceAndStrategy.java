package won.cryptography.ssl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.cryptography.service.keystore.KeyStoreService;

/**
 * User: ypanchenko Date: 12.08.2015
 *
 * This class is similar to the implementation of class TrustManagerDelegate of org.apache.http.conn.ssl
 * .SSLContextBuilder Unfortunately, they don't provide it as public class. It is useful when the default implementation
 * of X509KeyManager is used but additionally the strategy of how to choose the key when the key store contains many
 * keys is applied.
 *
 * For original see:
 * https://hc.apache.org/httpcomponents-client-4.4.x/httpclient/xref/org/apache/http/conn/ssl/SSLContextBuilder.html
 *
 */
public class KeyManagerWrapperWithKeyServiceAndStrategy implements X509KeyManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final X509KeyManager keyManager;
    private final PrivateKeyStrategy aliasStrategy;

    public KeyManagerWrapperWithKeyServiceAndStrategy(final KeyStoreService keyStoreService,
            final PrivateKeyStrategy aliasStrategy) {
        super();
        this.aliasStrategy = aliasStrategy;
        KeyManagerFactory kmf = null;
        try {
            kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStoreService.getUnderlyingKeyStore(), keyStoreService.getPassword().toCharArray());
        } catch (Exception e) {
            logger.error("KeyManager could not be initialized");
            throw new RuntimeException("KeyManager could not be initialized", e);
        }

        KeyManager[] kms = kmf.getKeyManagers();
        if (kms != null) {
            if (aliasStrategy != null) {
                for (int i = 0; i < kms.length; i++) {
                    KeyManager km = kms[i];
                    if (km instanceof X509KeyManager) {
                        this.keyManager = (X509KeyManager) km;
                        return;
                    }
                }
            }
        }
        // we found no X509KeyManager in manager factory key managers
        this.keyManager = null;
        throw new RuntimeException("X509KeyManager could not be initialized");
    }

    @Override
    public String[] getClientAliases(final String keyType, final Principal[] issuers) {
        return this.keyManager.getClientAliases(keyType, issuers);
    }

    @Override
    public String chooseClientAlias(final String[] keyTypes, final Principal[] issuers, final Socket socket) {
        final Map<String, PrivateKeyDetails> validAliases = new HashMap<String, PrivateKeyDetails>();
        for (final String keyType : keyTypes) {
            final String[] aliases = this.keyManager.getClientAliases(keyType, issuers);
            if (aliases != null) {
                for (final String alias : aliases) {
                    validAliases.put(alias, new PrivateKeyDetails(keyType, this.keyManager.getCertificateChain(alias)));
                }
            }
        }
        return this.aliasStrategy.chooseAlias(validAliases, socket);
    }

    @Override
    public String[] getServerAliases(final String keyType, final Principal[] issuers) {
        return this.keyManager.getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(final String keyType, final Principal[] issuers, final Socket socket) {
        final Map<String, PrivateKeyDetails> validAliases = new HashMap<String, PrivateKeyDetails>();
        final String[] aliases = this.keyManager.getServerAliases(keyType, issuers);
        if (aliases != null) {
            for (final String alias : aliases) {
                validAliases.put(alias, new PrivateKeyDetails(keyType, this.keyManager.getCertificateChain(alias)));
            }
        }
        return this.aliasStrategy.chooseAlias(validAliases, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(final String alias) {
        return this.keyManager.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(final String alias) {
        return this.keyManager.getPrivateKey(alias);
    }
}
