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

import won.owner.model.EmailVerificationToken;
import won.owner.model.KeyStoreIOException;
import won.owner.model.KeystoreHolder;
import won.owner.model.KeystorePasswordHolder;
import won.owner.model.User;
import won.owner.repository.EmailVerificationRepository;
import won.owner.repository.PersistentLoginRepository;
import won.owner.repository.UserRepository;

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

    /**
     * Transfers the specific user to a non existant new user with password
     *
     * @param newEmail
     * @param newPassword
     * @param privateUsername
     * @param privatePassword
     * @throws UserAlreadyExistsException                   when the new User already exists
     * @throws won.owner.service.impl.UserNotFoundException when the private User is not found
     */
    public User transferUser(String newEmail, String newPassword, String privateUsername, String privatePassword) throws UserAlreadyExistsException, UserNotFoundException {
        return transferUser(newEmail, newPassword, privateUsername, privatePassword, null);
    }

    /**
     * Transfers the specific user to a non existant new user with password and an optional role.
     *
     * @param newEmail
     * @param newPassword
     * @param privateUsername
     * @param privatePassword
     * @param role
     * @throws UserAlreadyExistsException                   when the new User already exists
     * @throws won.owner.service.impl.UserNotFoundException when the private User is not found
     */
    public User transferUser(String newEmail, String newPassword, String privateUsername, String privatePassword, String role) throws UserAlreadyExistsException, UserNotFoundException {
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

            //change the username/email and keystorpw holder
            privateUser.setUsername(newEmail);
            privateUser.setPassword(passwordEncoder.encode(newPassword));
            privateUser.setEmail(newEmail);
            privateUser.setEmailVerified(false);
            privateUser.setPrivateId(null);
            privateUser.setAcceptedTermsOfService(true); //transfer only available when flag is set therefore we can just set this to true (i think)
            if (role != null) {
                privateUser.setRole(role);
            }

            KeystorePasswordHolder privateKeystorePassword = privateUser.getKeystorePasswordHolder();

            String keystorePassword = privateKeystorePassword.getPassword(privatePassword);

            //************************************************
            KeystorePasswordHolder newKeystorePassword = new KeystorePasswordHolder();
            //generate a newPassword for the keystore and save it in the database, encrypted with a symmetric key
            //derived from the user's new password
            newKeystorePassword.setPassword(
                    keystorePassword,
                    newPassword);

            privateUser.setKeystorePasswordHolder(newKeystorePassword);
            save(privateUser);

            return privateUser;
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException();
        }
    }

    /**
     * Changes the specified user's password from old to new, changes user's key store password and invalidates all persistent logins.
     * 
     * @param username
     * @param newPassword
     * @param oldPassword
     * 
     * @throws UserNotFoundException when the private User is not found
     * @throws KeyStoreIOException if something goes wrong loading or saving the keystore
     * @throws WrongOldPasswordException if the old password is not the actual old password of the user 
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public User changePassword(String username, String newPassword, String oldPassword) throws UserNotFoundException, KeyStoreIOException, WrongOldPasswordException {
        logger.debug("changing password for user {}", username);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = getByUsernameWithKeystorePassword(username);
        if (user == null) {
            throw new UserNotFoundException();
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new WrongOldPasswordException();
        }
        KeystorePasswordHolder keystorePasswordHolder = user.getKeystorePasswordHolder();
        KeystoreHolder keystoreHolder = user.getKeystoreHolder();
        String oldKeystorePassword = keystorePasswordHolder.getPassword(oldPassword);
        String newKeystorePassword = KeystorePasswordUtils.generatePassword(KeystorePasswordUtils.KEYSTORE_PASSWORD_BYTES);
        KeyStore keyStore = keystoreHolder.getKeystore(oldKeystorePassword);
        //re-encrypt all private keys with the new password
        try {
            logger.debug("re-encrypting keystore for user {} with new keystore password", username);
            Enumeration aliases = keyStore.aliases();
            try {
                while(aliases.hasMoreElements()) {
                        String alias = (String) aliases.nextElement();
                        if (keyStore.isKeyEntry(alias)) {
                            Key key = keyStore.getKey(alias, oldKeystorePassword.toCharArray());
                            Certificate[] chain = keyStore.getCertificateChain(alias);
                            keyStore.setKeyEntry(alias, key, newKeystorePassword.toCharArray(), chain);
                        } else if (keyStore.isCertificateEntry(alias)) {
                            //ignore - certificates are not encrypted with a key
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
        // everything has worked so far, now make the changes
        user.setPassword(passwordEncoder.encode(newPassword));
        keystoreHolder.setKeystore(keyStore, newKeystorePassword);
        keystorePasswordHolder.setPassword(newKeystorePassword, newPassword);
        user.setKeystorePasswordHolder(keystorePasswordHolder);
        user.setKeystoreHolder(keystoreHolder);
        save(user);
        logger.debug("password changed for user {}", username);
        // persistent logins won't work any more as we changed the keystore password, so let's delete them
        persistentLoginRepository.deleteByUsername(username);
        return user;
    }

    
    /**
     * Registers the specified user with password and an optional role.
     * Assumes values have already been checked for syntactic validity.
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
     * Registers the specified user with password and an optional role.
     * Assumes values have already been checked for syntactic validity.
     *
     * @param email
     * @param password
     * @param role
     * @param isAnonymousUser
     * @throws UserAlreadyExistsException
     * @returns the created User
     */
    public User registerUser(String email, String password, String role, String privateId) throws UserAlreadyExistsException {
        User user = getByUsername(email);
        if (user != null) {
            throw new UserAlreadyExistsException();
        }
        try {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            user = new User(email, passwordEncoder.encode(password), role);
            user.setEmail(email);

            if(privateId != null) {
                user.setPrivateId(privateId);
            }

            user.setAcceptedTermsOfService(true); //transfer only available when flag is set therefore we can just set this to true (i think)
            KeystorePasswordHolder keystorePassword = new KeystorePasswordHolder();
            //generate a password for the keystore and save it in the database, encrypted with a symmetric key
            //derived from the user's password
            keystorePassword.setPassword(
                    KeystorePasswordUtils.generatePassword(KeystorePasswordUtils.KEYSTORE_PASSWORD_BYTES),
                    password);
            //keystorePassword = keystorePasswordRepository.save(keystorePassword);
            //generate the keystore for the user
            KeystoreHolder keystoreHolder = new KeystoreHolder();
            try {
                // create the keystore if it doesnt exist yet
                keystoreHolder.getKeystore(keystorePassword.getPassword(password));
            } catch (Exception e) {
                throw new IllegalStateException("could not create keystore for user " + email);
            }
            //keystoreHolder = keystoreHolderRepository.save(keystoreHolder);
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

        for(EmailVerificationToken token : tokens) {
            if(!token.isExpired()) return token;
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
}
