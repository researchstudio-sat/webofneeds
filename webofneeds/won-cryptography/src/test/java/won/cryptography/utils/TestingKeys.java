package won.cryptography.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import won.cryptography.service.keystore.FileBasedKeyStoreService;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ypanchenko Date: 12.04.2015
 */
public class TestingKeys {
    private final Map<String, PublicKey> publicKeys = new HashMap<>();
    private final Map<String, PrivateKey> privateKeys = new HashMap<>();

    public TestingKeys(final String keysFilePath) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        // load keys:
        File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
        FileBasedKeyStoreService storeService = new FileBasedKeyStoreService(keysFile, "temp");
        storeService.init();
        privateKeys.put(TestSigningUtils.atomCertUri, storeService.getPrivateKey(TestSigningUtils.atomCertUri));
        privateKeys.put(TestSigningUtils.ownerCertUri, storeService.getPrivateKey(TestSigningUtils.ownerCertUri));
        privateKeys.put(TestSigningUtils.nodeCertUri, storeService.getPrivateKey(TestSigningUtils.nodeCertUri));
        publicKeys.put(TestSigningUtils.atomCertUri,
                        storeService.getCertificate(TestSigningUtils.atomCertUri).getPublicKey());
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
