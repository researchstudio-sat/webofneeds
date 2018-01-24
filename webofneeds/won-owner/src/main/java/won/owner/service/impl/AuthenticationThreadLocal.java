package won.owner.service.impl;

import org.springframework.security.core.Authentication;

/**
 * Class that exposes the Authentication to the current thread.
 * Used for passing the Authentication from the WonWebsocketHandler
 * to the PerUserKeystoreService.
 * @author fkleedorfer
 *
 */
public class AuthenticationThreadLocal {
	private static final ThreadLocal<Authentication> AuthenticationThreadLocal = new ThreadLocal<>();
	
	public static boolean hasValue() {
		return AuthenticationThreadLocal.get() != null;
	}
	
	public static Authentication get() {
		return AuthenticationThreadLocal.get();
	}
	
	public static void set(Authentication auth) {
		AuthenticationThreadLocal.set(auth);
	}
	
	public static void remove() {
		AuthenticationThreadLocal.remove();
	}
}
