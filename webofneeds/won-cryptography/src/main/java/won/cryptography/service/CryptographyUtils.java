package won.cryptography.service;

import javax.crypto.Cipher;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class CryptographyUtils {

    public static boolean checkForUnlimitedSecurityPolicy() {

        try {
            int size = Cipher.getMaxAllowedKeyLength("RC5");
            System.out.println("max allowed key size: " + size);
            return  size < 256;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
