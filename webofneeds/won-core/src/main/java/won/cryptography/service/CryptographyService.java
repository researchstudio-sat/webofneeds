package won.cryptography.service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.cryptography.service.keystore.KeyStoreService;

/**
 * User: fsalcher Date: 12.06.2014
 */
public class CryptographyService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private KeyPairService keyPairService;
	private CertificateService certificateService;

	private KeyStoreService keyStoreService;

	private String defaultAlias;

	public CryptographyService(KeyStoreService keyStoreService) {
		this(keyStoreService,null);
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
			return;
		}
		// no certificate, create it:
		logger.info("certificate not found under alias {}, creating new one", defaultAlias);
		try {
			createNewKeyPair(defaultAlias, null);
			logger.info("certificate created");
		} catch (IOException e) {
			throw new RuntimeException("Could not create certificate for " + defaultAlias, e);
		}

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
}
