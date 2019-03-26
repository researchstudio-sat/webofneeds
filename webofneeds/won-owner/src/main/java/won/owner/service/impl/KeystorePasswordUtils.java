package won.owner.service.impl;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class KeystorePasswordUtils {
  public static final String CURRENT_VERSION = "v1";
  public static final int KEYSTORE_PASSWORD_BYTES = 32;

  public static String encryptPassword(String password, String key) {
    try {
      int iterations = 1000;
      byte[] salt = getSalt();
      byte[] iv = getSalt();
      PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), salt, iterations, 256);
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] encryptionKey = skf.generateSecret(spec).getEncoded();
      SecretKeySpec secretKey = new SecretKeySpec(encryptionKey, "AES");
      Cipher cipher = Cipher.getInstance(secretKey.getAlgorithm() + "/CFB8/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
      return CURRENT_VERSION + ":" + iterations + ":" + toHex(salt) + ":" + toHex(iv) + ":"
          + toHex(cipher.doFinal(password.getBytes()));
    } catch (Exception e) {
      throw new IllegalArgumentException("cannot encrypt password", e);
    }
  }

  public static String decryptPassword(String encrypted, String key) {
    try {
      String[] chunks = encrypted.split(":");
      String versionStr = chunks[0];
      String iterationsStr = chunks[1];
      String saltStr = chunks[2];
      String ivStr = chunks[3];
      String toDecryptStr = chunks[4];

      int iterations = Integer.valueOf(iterationsStr);
      byte[] salt = fromHex(saltStr);
      byte[] iv = fromHex(ivStr);
      byte[] toDecrypt = fromHex(toDecryptStr);
      PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), salt, iterations, 256);
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] encryptionKey = skf.generateSecret(spec).getEncoded();
      SecretKeySpec secretKey = new SecretKeySpec(encryptionKey, "AES");
      Cipher cipher = Cipher.getInstance(secretKey.getAlgorithm() + "/CFB8/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
      return new String(cipher.doFinal(toDecrypt));
    } catch (Exception e) {
      throw new IllegalArgumentException("cannot decrypt password", e);
    }
  }

  /**
   * Generates a 1000-fold hash of the specified string toHash, using saltString
   * as salt if non-null.
   * 
   * @param toHash
   * @param hashLength in bits
   * @param saltString must be a hexadecimal number
   * @return
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   */
  public static String generatePassword(String toHash, int hashLength) {
    try {
      int iterations = 1000;
      char[] chars = toHash.toCharArray();
      byte[] salt = getSalt();

      PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, hashLength);
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] hash = skf.generateSecret(spec).getEncoded();
      return CURRENT_VERSION + ":" + iterations + ":" + toHex(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      throw new RuntimeException("could not generate key", e);
    }
  }

  /**
   * Generates a random byte array of length passwordBytes, encoded as Base64
   * string.
   * 
   * @param passwordBytes
   * @return
   */
  public static String generatePassword(int passwordBytes) {
    try {
      SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
      byte[] password = new byte[passwordBytes];
      sr.nextBytes(password);
      return Base64.getEncoder().encodeToString(password);
    } catch (Exception e) {
      throw new RuntimeException("could not generate key", e);
    }
  }

  private static byte[] getSalt() throws NoSuchAlgorithmException {
    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
    byte[] salt = new byte[16];
    sr.nextBytes(salt);
    return salt;
  }

  private static byte[] padSalt(byte[] toPad) {
    byte[] salt = new byte[16];
    System.arraycopy(toPad, 0, salt, 0, Math.min(toPad.length, salt.length));
    return salt;
  }

  private static String toHex(byte[] array) {
    return DatatypeConverter.printHexBinary(array);
  }

  private static byte[] fromHex(String hexString) {
    return DatatypeConverter.parseHexBinary(hexString);
  }
}
