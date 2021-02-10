package won.auth;

import won.shacl2java.Shacl2JavaInstanceFactory;

public class WonAclInstanceFactory {
    private static ThreadLocal<Shacl2JavaInstanceFactory> instanceFactoryThreadLocal = new ThreadLocal<>();

    public static Shacl2JavaInstanceFactory get() {
        Shacl2JavaInstanceFactory factory = instanceFactoryThreadLocal.get();
        if (factory == null) {
            factory = AuthUtils.newInstanceFactory();
        }
        return factory;
    }
}
