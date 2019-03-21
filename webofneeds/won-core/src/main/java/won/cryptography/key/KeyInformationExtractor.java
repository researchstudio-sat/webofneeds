package won.cryptography.key;

import won.cryptography.exception.KeyNotSupportedException;

import java.security.Key;
import java.security.PublicKey;

/**
 * Helper interface for extracting information out of a (public key). Has to be
 * implemented for specific formats (e.g. Bouncy Castle).
 *
 * @author Fabian Salcher
 * @version 2014-07
 */
public interface KeyInformationExtractor {

  /**
   * returns the name of the algorithm the key will be used for
   *
   * @param key <code>Key</code> representing a private or public key
   * @return <code>String</code> with the name of the algorithm
   */
  public String getAlgorithm(Key key);

  /**
   * returns the ID of the elliptic curve the key will be used for
   *
   * @param key <code>Key</code> representing a private or public key
   * @return <code>String</code> with the ID of the curve
   */
  public String getCurveID(Key key) throws KeyNotSupportedException;

  /**
   * returns the x coordinate of the public point Q (also named W) of the public
   * key
   *
   * @param publicKey <code>PublicKey</code> representing the public key
   * @return <code>String</code> with base 16 notated number representing the x
   *         coordinate
   */
  public String getQX(PublicKey publicKey) throws KeyNotSupportedException;

  /**
   * returns the y coordinate of the public point Q (also named W) of the public
   * key
   *
   * @param publicKey <code>PublicKey</code> representing the public key
   * @return <code>String</code> with base 16 notated number representing the y
   *         coordinate
   */
  public String getQY(PublicKey publicKey) throws KeyNotSupportedException;

}
