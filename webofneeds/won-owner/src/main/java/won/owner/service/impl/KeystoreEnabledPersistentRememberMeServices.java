package won.owner.service.impl;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import won.owner.model.KeystoreHolder;
import won.owner.model.KeystorePasswordHolder;
import won.owner.model.PersistentLogin;
import won.owner.model.User;
import won.owner.repository.KeystorePasswordRepository;
import won.owner.repository.PersistentLoginRepository;

public class KeystoreEnabledPersistentRememberMeServices extends PersistentTokenBasedRememberMeServices {
	
	
	public KeystoreEnabledPersistentRememberMeServices(String key, UserDetailsService userDetailsService,
			PersistentTokenRepository tokenRepository) {
		super(key, userDetailsService, tokenRepository);
	}

	private static final String UNLOCK_COOKIE_NAME = "won.unlock";
	
	@Autowired
	private PersistentLoginRepository persistentLoginRepository;
	
	@Autowired 
	private KeystorePasswordRepository keystorePasswordRepository;
	
	@Autowired
	private PlatformTransactionManager platformTransactionManager;
	
	@Transactional
	protected UserDetails processAutoLoginCookie(String[] cookieTokens,
			HttpServletRequest request, HttpServletResponse response) {
	
		if (cookieTokens.length != 2) {
			throw new InvalidCookieException("Cookie token did not contain " + 2
					+ " tokens, but contained '" + Arrays.asList(cookieTokens) + "'");
		}

		final String presentedSeries = cookieTokens[0];
		final String presentedToken = cookieTokens[1];
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
		return transactionTemplate.execute(new TransactionCallback<UserDetails>() {
			@Override
			public UserDetails doInTransaction(TransactionStatus status) {
				PersistentLogin persistentLogin = persistentLoginRepository.findOne(presentedSeries);

				if (persistentLogin == null) {
					// No series match, so we can't authenticate using this cookie
					throw new RememberMeAuthenticationException(
							"No persistent token found for series id: " + presentedSeries);
				}

				// We have a match for this user/series combination
				if (!presentedToken.equals(persistentLogin.getToken())) {
					// Token doesn't match series value. Delete all logins for this user and throw
					// an exception to warn them.
					persistentLoginRepository.deleteByUsername(persistentLogin.getUsername());

					throw new CookieTheftException(
							messages.getMessage(
									"PersistentTokenBasedRememberMeServices.cookieStolen",
									"Invalid remember-me token (Series/token) mismatch. Implies previous cookie theft attack."));
				}

				if (persistentLogin.getLastUsed().getTime() + getTokenValiditySeconds() * 1000L < System
						.currentTimeMillis()) {
					throw new RememberMeAuthenticationException("Remember-me login has expired");
				}

				// Token also matches, so login is valid. Update the token value, keeping the
				// *same* series number.
				if (logger.isDebugEnabled()) {
					logger.debug("Refreshing persistent login token for user '"
							+ persistentLogin.getUsername() + "', series '" + persistentLogin.getSeries() + "'");
				}

				// ------------- begin: added for WoN  -----------------------
				// fetch the password from the keystore_password table 
				// using the value of the 'wonUnlock' coookie as key

				String unlockKey = extractUnlockCookie(request);
				if (unlockKey == null) {
					// we did not find the unlock cookie - something is wrong. 
					throw new CookieTheftException("The rememberMe cookie was ok but no unlock cookie was found.");	
				}
				
				KeystorePasswordHolder keystorePasswordHolder = persistentLogin.getKeystorePasswordHolder();
				String keystorePassword = keystorePasswordHolder.getPassword(unlockKey);
				
				// update the persistent login: new date, new token, and change unlock key for keystore password
				persistentLogin.setLastUsed(new Date());
				persistentLogin.setToken(generateTokenData());
				persistentLogin.setKeystorePasswordHolder(keystorePasswordHolder);
				String newUnlockKey= KeystorePasswordUtils.generatePassword(256);
				keystorePasswordHolder.setPassword(keystorePassword, newUnlockKey);
				try {
					persistentLoginRepository.save(persistentLogin);
					addCookies(persistentLogin, newUnlockKey, request, response);
				}
				catch (Exception e) {
					logger.error("Failed to update token: ", e);
					throw new RememberMeAuthenticationException(
							"Autologin failed due to data access problem");
				}

				User userDetails = (User) getUserDetailsService().loadUserByUsername(persistentLogin.getUsername());
				KeystoreHolder keystoreHolder = userDetails.getKeystoreHolder();
				KeyStore keystore;
				try {
					keystore = keystoreHolder.getKeystore(keystorePassword);
				} catch (Exception e) {
					logger.error("Failed to load keystore: ", e);
					throw new RememberMeAuthenticationException(
							"Autologin failed due to data access problem");
				}
				KeystoreEnabledUserDetails keystoreEnabledUserDetails = new KeystoreEnabledUserDetails((User)userDetails, keystore, keystorePassword);
				keystore = null;
				keystorePassword = null;
				return keystoreEnabledUserDetails;
				
				// delete the password 
			}
		});
		
		
	}
	
	/**
	 * Creates a new persistent login token with a new series number, stores the data in
	 * the persistent token repository and adds the corresponding cookie to the response.
	 *
	 */
	@Transactional
	protected void onLoginSuccess(HttpServletRequest request,
			HttpServletResponse response, Authentication successfulAuthentication) {
		String username = successfulAuthentication.getName();

		KeystoreEnabledUserDetails keystoreEnabledUserDetails = (KeystoreEnabledUserDetails) successfulAuthentication.getPrincipal();
		
		logger.debug("Creating new persistent login for user " + username);

		PersistentLogin persistentLogin = new PersistentLogin();
		persistentLogin.setUsername(username);
		persistentLogin.setSeries(generateSeriesData());
		persistentLogin.setToken(generateTokenData());
		persistentLogin.setLastUsed(new Date());
		
		String newUnlockKey = KeystorePasswordUtils.generatePassword(KeystorePasswordUtils.KEYSTORE_PASSWORD_BYTES);
		KeystorePasswordHolder keystorePasswordHolder = new KeystorePasswordHolder();
		keystorePasswordHolder.setPassword(keystoreEnabledUserDetails.getKeystorePassword(), newUnlockKey);
		persistentLogin.setKeystorePasswordHolder(keystorePasswordHolder);
		try {
			persistentLoginRepository.save(persistentLogin);
			addCookies(persistentLogin, newUnlockKey, request, response);
		}
		catch (Exception e) {
			logger.error("Failed to update token: ", e);
			throw new RememberMeAuthenticationException(
					"Autologin failed due to data access problem");
		}

	}
	
	private void addCookies(PersistentLogin persistentLogin, String key, HttpServletRequest request,
			HttpServletResponse response) {
		int validity = getTokenValiditySeconds();
		setCookie(new String[] { persistentLogin.getSeries(), persistentLogin.getToken() },
				validity, request, response);
		//set the unlock cookie
		setUnlockCookie(key, validity, request, response);
	}
	
	private String getCookiePath(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		return contextPath.length() > 0 ? contextPath : "/";
	}
	
	protected void setUnlockCookie(String value, int maxAge, HttpServletRequest request,
			HttpServletResponse response) {
		Cookie cookie = new Cookie(UNLOCK_COOKIE_NAME, value);
		cookie.setMaxAge(maxAge);
		cookie.setPath(getCookiePath(request));
		if (maxAge < 1) {
			cookie.setVersion(1);
		}
		cookie.setSecure(request.isSecure());
		response.addCookie(cookie);
	}
	
	protected String extractUnlockCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();

		if ((cookies == null) || (cookies.length == 0)) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (UNLOCK_COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
