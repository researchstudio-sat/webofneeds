package won.cryptography.service;

import java.security.Provider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class BCProvider {
    private static final Provider instance = new BouncyCastleProvider();

    public static Provider getInstance() {
        return instance;
    }
}
