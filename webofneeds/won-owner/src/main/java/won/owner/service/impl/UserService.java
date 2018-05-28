package won.owner.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import won.owner.model.KeystoreHolder;
import won.owner.model.KeystorePasswordHolder;
import won.owner.model.User;
import won.owner.repository.UserRepository;

/**
 * Created by fsuda on 28.05.2018.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;


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
    public void transferUser(String newEmail, String newPassword, String privateUsername, String privatePassword) throws UserAlreadyExistsException, UserNotFoundException {
        transferUser(newEmail, newPassword, privateUsername, privatePassword, null);
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
    public void transferUser(String newEmail, String newPassword, String privateUsername, String privatePassword, String role) throws UserAlreadyExistsException, UserNotFoundException {
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
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException();
        }
    }

    /**
     * Registers the specified user with password and an optional role.
     * Assumes values have already been checked for syntactic validity.
     *
     * @param email
     * @param password
     * @param role
     * @throws UserAlreadyExistsException
     */
    public void registerUser(String email, String password, String role) throws UserAlreadyExistsException {
        User user = getByUsername(email);
        if (user != null) {
            throw new UserAlreadyExistsException();
        }
        try {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            user = new User(email, passwordEncoder.encode(password), role);
            user.setEmail(email);
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

    public void save(User user) {
        userRepository.save(user);
    }
}
