package won.owner.service.impl;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;

import won.owner.model.KeystoreHolder;
import won.owner.model.User;
import won.owner.repository.KeystoreHolderRepository;
import won.owner.repository.UserRepository;

public class KeystoreEnabledDaoAuthenticationProvider extends DaoAuthenticationProvider {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired 
	KeystoreHolderRepository keystoreHolderRepository;
	
	@Override
	@Transactional
	public Authentication authenticate(Authentication authentication) {
		String password = (String) authentication.getCredentials();
		String username = (String) authentication.getPrincipal();
		UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) super.authenticate(authentication);
		User user = (User) auth.getPrincipal();
		//can't use that object as it's detached. load the user again:
		user = userRepository.findOne(user.getId());
		KeystoreHolder keystoreHolder = user.getKeystoreHolder();
		if (keystoreHolder == null) {
			//new user: create keystoreHolder
			keystoreHolder = new KeystoreHolder();
			user.setKeystoreHolder(keystoreHolder);
			keystoreHolder.setUser(user);
		}
		password = createPassword(username + password);
		KeyStore keystore = null;
		try {
			keystore = keystoreHolder.getKeystore(password);
		} catch (Exception e) {
			throw new IllegalStateException("could not open keystore for user " + auth.getName());
		}
		KeystoreUserDetails keystoreUserDetails = new KeystoreUserDetails();
		keystoreUserDetails.setKeyStore(keystore);
		keystoreUserDetails.setPassword(password);
		auth.setDetails(keystoreUserDetails);
		keystoreHolderRepository.save(keystoreHolder);
		userRepository.save(user);
		return auth;
	}
		
	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	public void setKeystoreHolderRepository(KeystoreHolderRepository keystoreHolderRepository) {
		this.keystoreHolderRepository = keystoreHolderRepository;
	}
	
	private String createPassword(String toHash) {
		 
		 String password = null;
		try {
			logger.info("creating new password...");
			password = generateHash(toHash);
			logger.info("done");
		} catch (Exception e) {
			logger.info("could not generate hash",e);
		} 
		return password;
	}
	
	private static String generateHash(String toHash) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        int iterations = 100;
        char[] chars = toHash.toCharArray();
        byte[] salt = getSalt();
         
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 50);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return iterations + ":" + toHex(salt) + ":" + toHex(hash);
    }
     
    private static byte[] getSalt() throws NoSuchAlgorithmException
    {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }
     
    private static String toHex(byte[] array) throws NoSuchAlgorithmException
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
        {
            return String.format("%0"  +paddingLength + "d", 0) + hex;
        }else{
            return hex;
        }
    }
}
