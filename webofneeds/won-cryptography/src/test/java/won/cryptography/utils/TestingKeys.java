package won.cryptography.utils;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import won.cryptography.service.keystore.FileBasedKeyStoreService;

/**
 * User: ypanchenko Date: 12.04.2015
 */
public class TestingKeys {
    private Map<String, PublicKey> publicKeys = new HashMap<>();
    private Map<String, PrivateKey> privateKeys = new HashMap<>();

    public TestingKeys(final String keysFilePath) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        // load keys:
        File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
        FileBasedKeyStoreService storeService = new FileBasedKeyStoreService(keysFile, "temp");
        storeService.init();
        privateKeys.put(TestSigningUtils.needCertUri, storeService.getPrivateKey(TestSigningUtils.needCertUri));
        privateKeys.put(TestSigningUtils.ownerCertUri, storeService.getPrivateKey(TestSigningUtils.ownerCertUri));
        privateKeys.put(TestSigningUtils.nodeCertUri, storeService.getPrivateKey(TestSigningUtils.nodeCertUri));
        publicKeys.put(TestSigningUtils.needCertUri,
                        storeService.getCertificate(TestSigningUtils.needCertUri).getPublicKey());
        publicKeys.put(TestSigningUtils.ownerCertUri,
                        storeService.getCertificate(TestSigningUtils.ownerCertUri).getPublicKey());
        publicKeys.put(TestSigningUtils.nodeCertUri,
                        storeService.getCertificate(TestSigningUtils.nodeCertUri).getPublicKey());
    }

    public Map<String, PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public PrivateKey getPrivateKey(final String keyUri) {
        return privateKeys.get(keyUri);
    }

    public PublicKey getPublicKey(final String keyUri) {
        return publicKeys.get(keyUri);
    }
}
