package won.cryptography.key;

import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.exception.KeyNotSupportedException;

import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;

/**
 * User: fsalcher Date: 24.07.2014
 */
public class KeyInformationExtractorBouncyCastle implements KeyInformationExtractor {
  private static final Logger logger = LoggerFactory.getLogger(KeyInformationExtractorBouncyCastle.class);

  public String getAlgorithm(Key key) {
    return key.getAlgorithm();
  }

  public String getCurveID(Key key) throws KeyNotSupportedException {
    if (key instanceof ECKey) {
      ECParameterSpec spec = ((ECKey) key).getParams();

      if (spec instanceof ECNamedCurveSpec) {
        return ((ECNamedCurveSpec) spec).getName();
      } else
        return null;
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
