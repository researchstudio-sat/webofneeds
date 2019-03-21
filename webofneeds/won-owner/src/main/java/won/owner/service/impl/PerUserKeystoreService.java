package won.owner.service.impl;

import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import won.cryptography.service.keystore.AbstractKeyStoreService;
import won.owner.model.User;
import won.owner.repository.KeystoreHolderRepository;
import won.protocol.util.AuthenticationThreadLocal;

@Component
public class PerUserKeystoreService extends AbstractKeyStoreService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private KeystoreHolderRepository keystoreHolderRepository;
	
	private String getUsername() {
		 return (String) SecurityContextHolder.getContext().getAuthentication().getName();
	}
	
	private Authentication getAuthentication() {
		if (AuthenticationThreadLocal.hasValue()) {
			return  (Authentication) AuthenticationThreadLocal.getAuthentication();
		} 
		return SecurityContextHolder.getContext().getAuthentication();
	}
	
	private KeystoreEnabledUserDetails getKeystoreUserDetails() {
		return (KeystoreEnabledUserDetails) getAuthentication().getPrincipal();
	}
	
	private User getUser() {
		return   ((KeystoreEnabledUserDetails) getAuthentication().getPrincipal()).getUser();
	}
	
	@Override
	public String getPassword() {
		 return getKeystoreUserDetails().getKeystorePassword();
	}
	
	

	@Override
	public KeyStore getUnderlyingKeyStore() {
		return getKeystoreUserDetails().getKeyStore();
	}

	@Override
	protected void persistStore() throws Exception {
		//fetch keystore and password from details in the authentication object 
		KeystoreEnabledUserDetails keystoreUserDetails = getKeystoreUserDetails();
		//write it back to the db
		User user = getUser();
		user.getKeystoreHolder().setKeystore(keystoreUserDetails.getKeyStore(), keystoreUserDetails.getKeystorePassword());
		keystoreHolderRepository.save(user.getKeystoreHolder());
	}
	
    
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}
    
    public void setKeystoreHolderRepository(KeystoreHolderRepository keystoreHolderRepository) {
		this.keystoreHolderRepository = keystoreHolderRepository;
	}

}
