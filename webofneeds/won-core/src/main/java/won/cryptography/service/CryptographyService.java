package won.cryptography.service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.cryptography.service.keystore.FileBasedKeyStoreService;
import won.cryptography.service.keystore.KeyStoreService;

/**
 * User: fsalcher Date: 12.06.2014
 */
public class CryptographyService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private KeyPairService keyPairService;
    private CertificateService certificateService;
    private KeyStoreService keyStoreService;
    @Autowired(required = false) // required only when keyToTrust* is configured
    private TrustStoreService trustStoreService;
    private String defaultAlias;
    // We want to be able to add keys from an additional key store to our trust
    // store at startup
    // this is required for the activemq broker to connect to itself: we force a
    // clien cert there,
    // which it provides happily - therefore we have to be able to trust it.
    private String keyToTrustFile;
    private String keyToTrustFilePassword;
    private String keyToTrustAlias = null;
    private String keyToTrustAliasUnder = null;
    private String keyToTrustProvider = null;
    private String keyToTrustKeystoreType = null;

    public CryptographyService(KeyStoreService keyStoreService) {
        this(keyStoreService, null);
    }

    public CryptographyService(KeyStoreService keyStoreService, String defaultAlias) {
        this(keyStoreService, new KeyPairService(), new CertificateService(), defaultAlias);
    }

    public CryptographyService(KeyStoreService keyStoreService, KeyPairService keyPairService,
                    CertificateService certificateService, String defaultAlias) {
        this.keyStoreService = keyStoreService;
        this.keyPairService = keyPairService;
        this.certificateService = certificateService;
        this.defaultAlias = defaultAlias;
    }

    @PostConstruct
    public void init() {
        createClientDefaultCertificateIfNotPresent();
    }

    /**
     * A default key (application acting as client key) has to be put into the key
     * store if not already present. This has to be done before other objects start
     * using CryptographyService or corresponding KeyStore.
     */
    private void createClientDefaultCertificateIfNotPresent() {
        if (defaultAlias == null)
            return;
        logger.debug("checking if the certificate with alias {} is in the keystore", defaultAlias);
        if (containsEntry(defaultAlias)) {
            logger.info("entry with alias {} found in the keystore", defaultAlias);
        } else {
            // no certificate, create it:
            logger.info("certificate not found under alias {}, creating new one", defaultAlias);
            try {
                createNewKeyPair(defaultAlias, null);
                logger.info("certificate created");
            } catch (IOException e) {
                throw new RuntimeException("Could not create certificate for " + defaultAlias, e);
            }
        }
        // uncomment for ssl handshake debugging
        // System.setProperty("javax.net.debug", "ssl");
        if (this.keyToTrustFile == null) {
            logger.info("no additional key configured to be imported into truststore");
            return;
        }
        FileBasedKeyStoreService keyToTrustKeyStoreService = new FileBasedKeyStoreService(new File(this.keyToTrustFile),
                        keyToTrustFilePassword, keyToTrustProvider, keyToTrustKeystoreType);
        try {
            keyToTrustKeyStoreService.init();
        } catch (Exception e) {
            logger.info("unable to read key for alias " + keyToTrustAlias + " from keystore " + keyToTrustFile, e);
        }
        Certificate cert = keyToTrustKeyStoreService.getCertificate(keyToTrustAlias);
        if (cert == null) {
            try {
                Optional<String> aliases = Collections.list(keyToTrustKeyStoreService.getUnderlyingKeyStore().aliases())
                                .stream().reduce((x, y) -> x + "," + y);
                logger.info("no key for alias {} found in keystore {}. Available aliases: {}",
                                new Object[] { keyToTrustAlias, keyToTrustFile, aliases.orElse("(none)") });
            } catch (Exception e) {
                logger.info("no key for alias " + keyToTrustAlias + " found in keystore " + keyToTrustFile
                                + "; caught exception while trying to log available aliases", e);
            }
            return;
        }
        // we need this so we can connect to ourself with ssl (used by the activemq
        // broker)
        logger.info("certificate with alias {} will be added/overwritten in truststore", keyToTrustAliasUnder);
        try {
            trustStoreService.addCertificate(keyToTrustAliasUnder, cert, true);
        } catch (Exception e) {
            logger.info("could not add certificate for alias " + keyToTrustAliasUnder + " to truststore", e);
        }
        logger.info("certificate with alias {} has been added to truststore", keyToTrustAliasUnder);
    }

    public KeyPair createNewKeyPair(BigInteger certNumber, String commonName, String webId) throws IOException {
        String alias = webId;
        if (alias == null) {
            alias = commonName;
        }
        // if (containsEntry(alias)) {
        // throw new IOException("Cannot create certificate - key store already contains
        // entry for " + alias);
        // }
        KeyPair newKeyPair = keyPairService.generateNewKeyPairInSecp384r1();
        X509Certificate newCertificate = certificateService.createSelfSignedCertificate(certNumber, newKeyPair,
                        commonName, webId);
        keyStoreService.putKey(alias, newKeyPair.getPrivate(), new Certificate[] { newCertificate }, false);
        return newKeyPair;
    }

    public KeyPair createNewKeyPair(String commonName, String webId) throws IOException {
        BigInteger certNumber = BigInteger.valueOf(1);
        return createNewKeyPair(certNumber, commonName, webId);
    }

    public PrivateKey getPrivateKey(String alias) {
        return keyStoreService.getPrivateKey(alias);
    }

    public PrivateKey getDefaultPrivateKey() {
        return keyStoreService.getPrivateKey(defaultAlias);
    }

    public String getDefaultPrivateKeyAlias() {
        return defaultAlias;
    }

    public PublicKey getPublicKey(String alias) {
        return keyStoreService.getPublicKey(alias);
    }

    public boolean containsEntry(String alias) {
        try {
            return keyStoreService.getUnderlyingKeyStore().containsAlias(alias);
        } catch (KeyStoreException e) {
            return false;
        }
    }

    public void setDefaultAlias(String defaultAlias) {
        this.defaultAlias = defaultAlias;
    }

    public void setTrustStoreService(TrustStoreService trustStoreService) {
        this.trustStoreService = trustStoreService;
    }

    public void setKeyToTrustAlias(String keyToTrustAlias) {
        this.keyToTrustAlias = keyToTrustAlias;
    }

    public void setKeyToTrustAliasUnder(String keyToTrustAliasUnder) {
        this.keyToTrustAliasUnder = keyToTrustAliasUnder;
    }

    public void setKeyToTrustFile(String keyToTrustFile) {
        this.keyToTrustFile = keyToTrustFile;
    }

    public void setKeyToTrustFilePassword(String keyToTrustFilePassword) {
        this.keyToTrustFilePassword = keyToTrustFilePassword;
    }

    public void setKeyToTrustKeystoreType(String keyToTrustKeystoreType) {
        this.keyToTrustKeystoreType = keyToTrustKeystoreType;
    }

    public void setKeyToTrustProvider(String keyToTrustProvider) {
        this.keyToTrustProvider = keyToTrustProvider;
    }
}
