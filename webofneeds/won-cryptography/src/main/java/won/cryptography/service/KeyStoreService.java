package won.cryptography.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileLock;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Arrays;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class KeyStoreService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
    private static final String KEY_STORE_TYPE = "UBER";

    // ToDo: password should really not be here! (FS)
    private final char[] storePW = "temp".toCharArray();

    // ToDo: load from config file (FS)

    private File storeFile;

    private java.security.KeyStore store;

    public KeyStoreService(String filePath) {
      this(new File(filePath));
    }

    public KeyStoreService(File storeFile) {

        Security.addProvider(new BouncyCastleProvider());

        this.storeFile = storeFile;

        try {
            store = java.security.KeyStore.getInstance(KEY_STORE_TYPE, PROVIDER_BC);

            if (storeFile == null || !storeFile.exists() || !storeFile.isFile())
                store.load(null, null);
            else {
                loadStoreFromFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PrivateKey getPrivateKey(String alias) {

        PrivateKey retrieved = null;

        try {
            retrieved = (PrivateKey) store.getKey(alias, storePW);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retrieved;

    }

    public PublicKey getPublicKey(String alias) {

      PublicKey retrieved = null;

      try {
        retrieved = getCertificate(alias).getPublicKey();
      } catch (Exception e) {
        e.printStackTrace();
      }

      return retrieved;

    }

    public Certificate getCertificate(String alias) {

      Certificate retrieved = null;

      try {
        retrieved = store.getCertificate(alias);
      } catch (Exception e) {
        e.printStackTrace();
      }

      return retrieved;

    }

    public synchronized void putKey(String alias, Key key, Certificate[] certificateChain) {

        try {
            store.setKeyEntry(alias, key, storePW, certificateChain);
            saveStoreToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private synchronized void saveStoreToFile() {

        FileOutputStream outputStream = null;
      //TODO the lock seem to not work. Anyway, we wanted to change keystore to be generated per web app,
      //then we will not have to lock the keystore file at all.
        FileLock lock = null;

        try {

            outputStream = new FileOutputStream(storeFile);
            lock = outputStream.getChannel().lock();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (outputStream != null) {
            try {

                store.store(outputStream, Arrays.copyOf(storePW, storePW.length));

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (lock != null) {
                  try {
                    if (lock.isValid()) {
                      lock.release();
                    } else {
                      logger.warn("Keystore file lock was not valid!");
                    }

                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void loadStoreFromFile() {

        FileInputStream inputStream = null;

        try {

            inputStream = new FileInputStream(storeFile);

        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }

      if (inputStream != null) {
            try {

                store.load(inputStream, Arrays.copyOf(storePW, storePW.length));

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

  public int size() {
    try {
      return store.size();
    } catch (KeyStoreException e) {
      //TODO proper logging
      logger.warn(e.toString());
    }
    return 0;
  }
}
