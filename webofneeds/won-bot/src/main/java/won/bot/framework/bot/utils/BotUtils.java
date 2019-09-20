package won.bot.framework.bot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.lang.invoke.MethodHandles;
import java.security.NoSuchAlgorithmException;

public class BotUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Checks if a Bot can be executed based on the mandatory set of parameters, and
     * the currently available Encryption strength if forceMandatory is not set, the
     * WON_KEYSTORE_DIR parameter will have a fallback to a default value
     *
     * @param forceMandatory if set to true, every checked attribute will have to be
     * set
     * @return true if the configuration is valid, false if it is not
     */
    public static boolean isValidRunConfig(boolean forceMandatory) {
        boolean valid = true;
        try {
            if (Cipher.getMaxAllowedKeyLength("AES") != Integer.MAX_VALUE) {
                logger.error("JCE unlimited strength encryption policy is not enabled, WoN applications will not work. Please consult the setup guide.");
                valid = false;
            }
        } catch (NoSuchAlgorithmException e) {
            logger.error("ALGORITHM AES is not present, we can't check the encryption policy strength and assume that it is not sufficient. Please consult the setup guide.");
            valid = false;
        }
        if (System.getProperty("WON_NODE_URI") == null && System.getenv("WON_NODE_URI") == null) {
            logger.error("WON_NODE_URI needs to be set to the node you want to connect to. e.g. https://hackathonnode.matchat.org/won");
            valid = false;
        }
        if (System.getProperty("WON_KEYSTORE_DIR") == null && System.getenv("WON_KEYSTORE_DIR") == null) {
            if (forceMandatory) {
                logger.warn("WON_KEYSTORE_DIR is not set, e.g. \"./\" (to set it to the working directory of the bot) ");
                valid = false;
            } else {
                logger.warn("WON_KEYSTORE_DIR is not set, setting it to the current working directory");
                System.setProperty("WON_KEYSTORE_DIR", "./");
            }
        }
        return valid;
    }

    /**
     * Checks if a Bot can be executed based on the mandatory set of parameters, and
     * the currently available Encryption strength The WON_KEYSTORE_DIR parameter
     * will be seen as optional, and will be set to the current working directory if
     * it is not present.
     * 
     * @return true if the configuration is valid, false if it is not
     */
    public static boolean isValidRunConfig() {
        return isValidRunConfig(false);
    }
}
