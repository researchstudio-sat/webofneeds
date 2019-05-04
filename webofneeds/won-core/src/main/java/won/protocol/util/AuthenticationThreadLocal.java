package won.protocol.util;

/**
 * Class that exposes the Authentication to the current thread. Used for passing
 * the Authentication from the WonWebsocketHandler to the
 * PerUserKeystoreService. The type Object is used instead
 * <code>of org.springframework.security.core.Authentication</code> because this
 * class needs to be in won-core, and won-core should not depend on
 * spring-security.
 * 
 * @author fkleedorfer
 */
public class AuthenticationThreadLocal {
    private static final ThreadLocal<Object> AuthenticationThreadLocal = new ThreadLocal<>();

    public static boolean hasValue() {
        return AuthenticationThreadLocal.get() != null;
    }

    public static Object getAuthentication() {
        return AuthenticationThreadLocal.get();
    }

    public static void setAuthentication(Object authentication) {
        AuthenticationThreadLocal.set(authentication);
    }

    public static void remove() {
        AuthenticationThreadLocal.remove();
    }
}
