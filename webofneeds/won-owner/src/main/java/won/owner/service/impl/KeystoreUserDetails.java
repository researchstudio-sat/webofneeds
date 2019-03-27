package won.owner.service.impl;

import java.security.KeyStore;

public class KeystoreUserDetails {
    private KeyStore keyStore;
    private String password;

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
