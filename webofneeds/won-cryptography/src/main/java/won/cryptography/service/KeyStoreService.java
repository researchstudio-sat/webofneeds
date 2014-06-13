package won.cryptography.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.Key;
import java.security.Security;
import java.security.cert.Certificate;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class KeyStoreService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
    private static final String KEY_STORE_TYPE = "UEBER";

    // ToDo: password should not be here
    private final char[] storePW = "temp".toCharArray();

    // ToDo: load from config file


    private File storeFile;

    private java.security.KeyStore store;

    public KeyStoreService(File storeFile) {

        Security.addProvider(new BouncyCastleProvider());

        this.storeFile = storeFile;

        try {
            store = java.security.KeyStore.getInstance(KEY_STORE_TYPE, PROVIDER_BC);

            if (storeFile == null)
                store.load(null, null);
            else {
                loadStoreFromFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Key getKey(String alias) {

        Key retrieved = null;

        try {
            retrieved = store.getKey("key1", storePW);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retrieved;

    }

    public void putKey(String alias, Key key, Certificate[] certificateChain) {


        try {
            store.setKeyEntry(alias, key, storePW, certificateChain);
            saveStoreToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void saveStoreToFile() {

        OutputStream outputStream = null;

        try {

            outputStream = new FileOutputStream(storeFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (outputStream != null) {
            try {

                store.store(outputStream, storePW);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void loadStoreFromFile() {

        InputStream inputStream = null;

        try {

            inputStream = new FileInputStream(storeFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (inputStream != null) {
            try {

                store.load(inputStream, storePW);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
