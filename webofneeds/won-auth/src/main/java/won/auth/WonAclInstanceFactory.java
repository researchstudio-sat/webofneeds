package won.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.lang.invoke.MethodHandles;

public class WonAclInstanceFactory {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static ThreadLocal<Shacl2JavaInstanceFactory> instanceFactoryThreadLocal = new ThreadLocal<>();

    public static Shacl2JavaInstanceFactory get() {
        Shacl2JavaInstanceFactory factory = instanceFactoryThreadLocal.get();
        if (factory == null) {
            logger.debug("Creating new thread-local instance factory for package won.auth.model..");
            long start = System.currentTimeMillis();
            factory = AuthUtils.newInstanceFactory();
            logger.debug("creating instance factory took {} millis", System.currentTimeMillis() - start);
            instanceFactoryThreadLocal.set(factory);
        }
        return factory;
    }
}
