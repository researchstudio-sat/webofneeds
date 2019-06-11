package won.owner.service.impl;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import won.owner.model.*;
import won.owner.repository.*;
import won.protocol.util.ExpensiveSecureRandomString;

/**
 * Created by fsuda on 28.05.2018.
 */
@Service
public class UserService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailVerificationRepository emailVerificationRepository;
    @Autowired
    private PersistentLoginRepository persistentLoginRepository;
    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;
    @Autowired
    private KeystorePasswordRepository keystorePasswordRepository;
    private ExpensiveSecureRandomString randomStringGenerator = new ExpensiveSecureRandomString();

    /**
     * Transfers the specific user to a non existant new user with password
     *
     * @param newEmail
     * @param newPassword
     * @param privateUsername
     * @param privatePassword
     * @throws UserAlreadyExistsException when the new User already exists
     * @throws won.owner.service.impl.UserNotFoundException when the private User is
     * not found
     */
    public User transferUser(String newEmail, String newPassword, String privateUsername, String privatePassword)
                    throws UserAlreadyExistsException, UserNotFoundException {
        return transferUser(newEmail, newPassword, privateUsername, privatePassword, null);
    }

    /**
     * Transfers the specific user to a non existant new user with password and an
     * optional role.
     *
     * @param newEmail
     * @param newPassword
     * @param privateUsername
     * @param privatePassword
     * @param role
     * @throws UserAlreadyExistsException when the new User already exists
     * @throws won.owner.service.impl.UserNotFoundException when the private User is
     * not found
     */
    public User transferUser(String newEmail, String newPassword, String privateUsername, String privatePassword,
                    String role) throws UserAlreadyExistsException, UserNotFoundException {
        User user = getByUsername(newEmail);
        if (user != null) {
            throw new UserAlreadyExistsException();
        }
        try {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            User privateUser = getByUsernameWithKeystorePassword(privateUsername);
            if (privateUser == null) {
                throw new UserNotFoundException();
            }
            // change the username/email and keystorpw holder
            privateUser.setUsername(newEmail);
            privateUser.setPassword(passwordEncoder.encode(newPassword));
            privateUser.setEmail(newEmail);
            privateUser.setEmailVerified(false);
            privateUser.setPrivateId(null);
            privateUser.setAcceptedTermsOfService(true); // transfer only available when flag is set therefore we can
                                                         // just set
                                                         // this to true (i think)
            if (role != null) {
                privateUser.setRole(role);
            }
            KeystorePasswordHolder privateKeystorePassword = privateUser.getKeystorePasswordHolder();
            String keystorePassword = privateKeystorePassword.getPassword(privatePassword);
            // ************************************************
            KeystorePasswordHolder newKeystorePassword = new KeystorePasswordHolder();
            // generate a newPassword for the keystore and save it in the database,
            // encrypted with a symmetric key
            // derived from the user's new password
            newKeystorePassword.setPassword(keystorePassword, newPassword);
            privateUser.setKeystorePasswordHolder(newKeystorePassword);
            // we delete the recoverable keystore key as it will no longer work
            privateUser.setRecoverableKeystorePasswordHolder(null);
            save(privateUser);
            return privateUser;
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException();
        }
    }

    /**
     * Changes the specified user's password from old to new, changes user's key
     * store password and invalidates all persistent logins.
     * 
     * @param username
     * @param newPassword
     * @param oldPassword
     * @throws UserNotFoundException when the private User is not found
     * @throws KeyStoreIOException if something goes wrong loading or saving the
     * keystore
     * @throws IncorrectPasswordException if the old password is not the actual old
     * password of the user
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public User changePassword(String username, String newPassword, String oldPassword)
                    throws UserNotFoundException, KeyStoreIOException, IncorrectPasswordException {
        logger.debug("changing password for user {}", username);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = getByUsernameWithKeystorePassword(username);
        if (user == null) {
            throw new UserNotFoundException("cannot change password: user not found");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IncorrectPasswordException("cannot change password: old password is incorrect");
        }
        KeystorePasswordHolder keystorePasswordHolder = user.getKeystorePasswordHolder();
        String oldKeystorePassword = keystorePasswordHolder.getPassword(oldPassword);
        logger.debug("re-encrypting keystore for user {} with new keystore password", username);
        String newKeystorePassword = changeKeystorePassword(user, oldKeystorePassword);
        // everything has worked so far, now make the changes
        user.setPassword(passwordEncoder.encode(newPassword));
        keystorePasswordHolder.setPassword(newKeystorePassword, newPassword);
        user.setKeystorePasswordHolder(keystorePasswordHolder);
        // we delete the recoverable keystore key as it will no longer work
        user.setRecoverableKeystorePasswordHolder(null);
        save(user);
        logger.debug("password changed for user {}", username);
        // persistent logins won't work any more as we changed the keystore password, so
        // let's delete them
        persistentLoginRepository.deleteByUsername(username);
        return user;
    }

    /**
     * Generates a new recovery key for the user
     **/
    @Transactional(propagation = Propagation.REQUIRED)
    public String generateRecoveryKey(String email, String password)
                    throws UserNotFoundException, IncorrectPasswordException {
        logger.debug("changing password for user {}", email);
        User user = getByUsernameWithKeystorePassword(email);
        if (user == null) {
            throw new UserNotFoundException("cannot generate recovery key: user not found");
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IncorrectPasswordException("cannot generate recovery key: incorrect password");
        }
        KeystorePasswordHolder keystorePasswordHolder = user.getKeystorePasswordHolder();
        KeystoreHolder keystoreHolder = user.getKeystoreHolder();
        String keystorePassword = keystorePasswordHolder.getPassword(password);
        StringBuilder sb = new StringBuilder();
        sb.append("MY__").append(randomStringGenerator.nextString(4)).append("_")
                        .append(randomStringGenerator.nextString(4)).append("_")
                        .append(randomStringGenerator.nextString(4)).append("_")
                        .append(randomStringGenerator.nextString(4)).append("__KEY");
        String recoveryKey = sb.toString();
        KeystorePasswordHolder recoverableKeystorePasswordHolder = new KeystorePasswordHolder();
        recoverableKeystorePasswordHolder.setPassword(keystorePassword, recoveryKey);
        keystorePasswordRepository.save(recoverableKeystorePasswordHolder);
        user.setRecoverableKeystorePasswordHolder(recoverableKeystorePasswordHolder);
        userRepository.save(user);
        return recoveryKey;
    }

    /**
     * Uses the recoveryKey to unlock the keystore password, then generates a new
     * keystore password and if that all works, changes the user's password and
     * deletes the recovery key.
     **/
    @Transactional(propagation = Propagation.REQUIRED)
    public User useRecoveryKey(String username, String newPassword, String recoveryKey)
                    throws UserNotFoundException, KeyStoreIOException, IncorrectPasswordException {
        logger.debug("using recoery key to reset password for user {}", username);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = getByUsernameWithKeystorePassword(username);
        if (user == null) {
            throw new UserNotFoundException("cannot change password: user not found");
        }
        KeystorePasswordHolder keystorePasswordHolder = user.getRecoverableKeystorePasswordHolder();
        String oldKeystorePassword = keystorePasswordHolder.getPassword(recoveryKey);
        logger.debug("re-encrypting keystore for user {} with new keystore password", username);
        String newKeystorePassword = changeKeystorePassword(user, oldKeystorePassword);
        user.setKeystorePasswordHolder(keystorePasswordHolder);
        user.getKeystorePasswordHolder().setPassword(newKeystorePassword, newPassword);
        // everything has worked so far, now we can also change the user's password
        user.setPassword(passwordEncoder.encode(newPassword));
        // we delete the recoverable keystore key as it will no longer work
        user.setRecoverableKeystorePasswordHolder(null);
        save(user);
        logger.debug("password changed for user {}", username);
        // persistent logins won't work any more as we changed the keystore password, so
        // let's delete them
        persistentLoginRepository.deleteByUsername(username);
        return user;
    }

    /**
     * Changes the keystore password, re-encrypting the private keys with the new
     * password.
     * 
     * @return the new keystore password
     */
    private String changeKeystorePassword(User user, String oldKeystorePassword) throws KeyStoreIOException {
        String newKeystorePassword = KeystorePasswordUtils
                        .generatePassword(KeystorePasswordUtils.KEYSTORE_PASSWORD_BYTES);
        KeyStore keyStore = user.getKeystoreHolder().getKeystore(oldKeystorePassword);
        // re-encrypt all private keys with the new password
        try {
            Enumeration aliases = keyStore.aliases();
            try {
                while (aliases.hasMoreElements()) {
                    String alias = (String) aliases.nextElement();
                    if (keyStore.isKeyEntry(alias)) {
                        Key key = keyStore.getKey(alias, oldKeystorePassword.toCharArray());
                        Certificate[] chain = keyStore.getCertificateChain(alias);
                        keyStore.setKeyEntry(alias, key, newKeystorePassword.toCharArray(), chain);
                    } else if (keyStore.isCertificateEntry(alias)) {
                        // ignore - certificates are not encrypted with a key
                    }
                    logger.debug("re-encrypted key for alias: {} ", alias);
                }
            } catch (UnrecoverableKeyException e) {
                throw new KeyStoreIOException("could not re-encrypt key", e);
            } catch (NoSuchAlgorithmException e) {
                throw new KeyStoreIOException("could not re-encrypt key", e);
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreIOException("could not re-encrypt key", e);
        }
        user.getKeystoreHolder().setKeystore(keyStore, newKeystorePassword);
        return newKeystorePassword;
    }

    /**
     * Registers the specified user with password and an optional role. Assumes
     * values have already been checked for syntactic validity.
     *
     * @param email
     * @param password
     * @param role
     * @throws UserAlreadyExistsException
     * @returns the created User
     */
    public User registerUser(String email, String password, String role) throws UserAlreadyExistsException {
        return registerUser(email, password, role, null);
    }

    /**
     * Registers the specified user with password and an optional role. Assumes
     * values have already been checked for syntactic validity.
     *
     * @param email
     * @param password
     * @param role
     * @param isAnonymousUser
     * @throws UserAlreadyExistsException
     * @returns the created User
     */
    public User registerUser(String email, String password, String role, String privateId)
                    throws UserAlreadyExistsException {
        User user = getByUsername(email);
        if (user != null) {
            throw new UserAlreadyExistsException();
        }
        try {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            user = new User(email, passwordEncoder.encode(password), role);
            user.setEmail(email);
            if (privateId != null) {
                user.setPrivateId(privateId);
            }
            user.setAcceptedTermsOfService(true); // transfer only available when flag is set therefore we can just set
                                                  // this
                                                  // to true (i think)
            KeystorePasswordHolder keystorePassword = new KeystorePasswordHolder();
            // generate a password for the keystore and save it in the database, encrypted
            // with a symmetric key
            // derived from the user's password
            keystorePassword.setPassword(
                            KeystorePasswordUtils.generatePassword(KeystorePasswordUtils.KEYSTORE_PASSWORD_BYTES),
                            password);
            // keystorePassword = keystorePasswordRepository.save(keystorePassword);
            // generate the keystore for the user
            KeystoreHolder keystoreHolder = new KeystoreHolder();
            try {
                // create the keystore if it doesnt exist yet
                keystoreHolder.getKeystore(keystorePassword.getPassword(password));
            } catch (Exception e) {
                throw new IllegalStateException("could not create keystore for user " + email);
            }
            // keystoreHolder = keystoreHolderRepository.save(keystoreHolder);
            user.setKeystorePasswordHolder(keystorePassword);
            user.setKeystoreHolder(keystoreHolder);
            save(user);
            return user;
        } catch (DataIntegrityViolationException e) {
            // username is already in database
            throw new UserAlreadyExistsException();
        }
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getByUsernameWithKeystorePassword(String username) {
        return userRepository.findByUsernameWithKeystorePassword(username);
    }

    public EmailVerificationToken getEmailVerificationToken(String verificationToken) {
        return emailVerificationRepository.findByToken(verificationToken);
    }

    public EmailVerificationToken getEmailVerificationToken(User user) {
        List<EmailVerificationToken> tokens = emailVerificationRepository.findByUser(user);
        for (EmailVerificationToken token : tokens) {
            if (!token.isExpired())
                return token;
        }
        return null;
    }

    public EmailVerificationToken createEmailVerificationToken(User user) {
        return createEmailVerificationToken(user, UUID.randomUUID().toString());
    }

    public EmailVerificationToken createEmailVerificationToken(User user, String verificationToken) {
        EmailVerificationToken token = new EmailVerificationToken(user, verificationToken);
        emailVerificationRepository.save(token);
        return token;
    }

    public User getUserByEmailVerificationToken(String verificationToken) {
        return getEmailVerificationToken(verificationToken).getUser();
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public void addPushSubscription(User user, PushSubscription subscription) {
        pushSubscriptionRepository.save(subscription);
        user.addPushSubscription(subscription);
    }
}
