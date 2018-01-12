package won.owner.service.impl;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import won.cryptography.service.keystore.AbstractKeyStoreService;
import won.owner.model.User;
import won.owner.repository.UserRepository;

@Component
public class PerUserKeystoreService extends AbstractKeyStoreService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired 
	private UserRepository UserRepository;
	
	private String getUsername() {
		 return (String) SecurityContextHolder.getContext().getAuthentication().getName();
	}
	
	
	private KeystoreUserDetails getKeystoreUserDetails() {
		 return (KeystoreUserDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
	}
	
	private User getUser() {
		return  (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
	
	@Override
	public String getPassword() {
		 return getKeystoreUserDetails().getPassword();
	}
	
	

	@Override
	public KeyStore getUnderlyingKeyStore() {
		return getKeystoreUserDetails().getKeyStore();
	}

	@Override
	protected void persistStore() throws Exception {
		//fetch keystore and password from details in the authentication object 
		KeystoreUserDetails keystoreUserDetails = getKeystoreUserDetails();
		//write it back to the db
		getUser().getKeystoreHolder().setKeystore(keystoreUserDetails.getKeyStore(), keystoreUserDetails.getPassword());
	}
	
    
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

}
