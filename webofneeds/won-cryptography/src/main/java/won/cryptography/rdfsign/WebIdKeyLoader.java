package won.cryptography.rdfsign;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;

public interface WebIdKeyLoader {
    Set<PublicKey> loadKey(String keyURI)
                    throws NoSuchAlgorithmException, NoSuchProviderException,
                    InvalidKeySpecException;
}
