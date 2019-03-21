package won.owner.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import won.owner.model.KeystoreHolder;
import won.owner.model.KeystorePasswordHolder;
import won.owner.model.User;
import won.owner.repository.KeystoreHolderRepository;
import won.owner.repository.KeystorePasswordRepository;
import won.owner.repository.UserRepository;

import javax.transaction.Transactional;
import java.security.KeyStore;

public class KeystoreEnabledDaoAuthenticationProvider extends DaoAuthenticationProvider {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired UserRepository userRepository;

  @Autowired KeystoreHolderRepository keystoreHolderRepository;

  @Autowired KeystorePasswordRepository keystorePasswordRepository;

  @Override @Transactional public Authentication authenticate(Authentication authentication) {
    String password = (String) authentication.getCredentials();
    String username = (String) authentication.getPrincipal();
    UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) super.authenticate(authentication);
    User user = (User) auth.getPrincipal();
    //can't use that object as it's detached. load the user again:
    user = userRepository.findOne(user.getId());
    KeystorePasswordHolder keystorePasswordHolder = user.getKeystorePasswordHolder();
    if (keystorePasswordHolder == null || keystorePasswordHolder.getEncryptedPassword() == null
        || keystorePasswordHolder.getEncryptedPassword().length() == 0) {
      keystorePasswordHolder = new KeystorePasswordHolder();
      //generate a password for the keystore and save it in the database, encrypted with a symmetric key
      //derived from the user's password
      keystorePasswordHolder
          .setPassword(KeystorePasswordUtils.generatePassword(KeystorePasswordUtils.KEYSTORE_PASSWORD_BYTES), password);
      //keystorePasswordHolder = keystorePasswordRepository.save(keystorePasswordHolder);
      //generate the keystore for the user
      user.setKeystorePasswordHolder(keystorePasswordHolder);
    }
    String keystorePassword = keystorePasswordHolder.getPassword(password);
    KeystoreHolder keystoreHolder = user.getKeystoreHolder();
    KeyStore keystore = null;
    if (keystoreHolder == null || keystoreHolder.getKeystoreBytes() == null
        || keystoreHolder.getKeystoreBytes().length == 0) {
      //new user or legacy user that has no keystore yet: create keystoreHolder
      keystoreHolder = new KeystoreHolder();
      keystore = openOrCreateKeyStore(keystorePassword, auth.getName(), keystoreHolder);
      //keystoreHolder = keystoreHolderRepository.save(keystoreHolder);
      user.setKeystoreHolder(keystoreHolder);
    } else {
      try {
        keystore = keystoreHolder.getKeystore(keystorePassword);
      } catch (Exception e) {
        throw new IllegalStateException("could not open keystore for user " + username);
      }
    }
    userRepository.save(user);
    KeystoreEnabledUserDetails ud = new KeystoreEnabledUserDetails(user, keystore, keystorePassword);
    return new UsernamePasswordAuthenticationToken(ud, null, auth.getAuthorities());
  }

  private KeyStore openOrCreateKeyStore(String password, String username, KeystoreHolder keystoreHolder) {
    KeyStore keystore = null;
    try {
      keystore = keystoreHolder.getKeystore(password);
    } catch (Exception e) {
      throw new IllegalStateException("could not open keystore for user " + username);
    }
    return keystore;
  }

  public void setUserRepository(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public void setKeystoreHolderRepository(KeystoreHolderRepository keystoreHolderRepository) {
    this.keystoreHolderRepository = keystoreHolderRepository;
  }

}
