package won.cryptography.key;

import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;

import won.cryptography.exception.KeyNotSupportedException;

/**
 * User: fsalcher Date: 24.07.2014
 */
public class KeyInformationExtractorBouncyCastle implements KeyInformationExtractor {
    public String getAlgorithm(Key key) {
        return key.getAlgorithm();
    }

    public String getCurveID(Key key) throws KeyNotSupportedException {
        if (key instanceof ECKey) {
            // doing this in a weird way. We changed this from an instanceof check for the
            // ECParameterSpec class, which, due to some classloader magic gone wrong failed
            // for the object which actually is of that class. I guess that the class is in
            // multiple jars loaded from different classloaders... hard to know.
            // This works.
            ECParameterSpec spec = ((ECKey) key).getParams();
            Class<?> clazz = spec.getClass();
            try {
                return (String) clazz.getDeclaredMethod("getName").invoke(spec);
            } catch (Exception e) {
                throw new KeyNotSupportedException("Cannot get curve name from ECParameterSpec " + spec, e);
            }
        } else {
            throw new KeyNotSupportedException("Key is not an elliptic curve key!");
        }
    }

    public String getQX(PublicKey publicKey) throws KeyNotSupportedException {
        if (publicKey instanceof ECPublicKey) {
            return ((ECPublicKey) publicKey).getW().getAffineX().toString(16);
        } else {
            throw new KeyNotSupportedException("Key is not an elliptic curve key!");
        }
    }

    public String getQY(PublicKey publicKey) throws KeyNotSupportedException {
        if (publicKey instanceof ECPublicKey) {
            return ((ECPublicKey) publicKey).getW().getAffineY().toString(16);
        } else {
            throw new KeyNotSupportedException("Key is not an elliptic curve key!");
        }
    }
}
