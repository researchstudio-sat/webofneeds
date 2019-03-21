package won.protocol.util;

import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * Created with IntelliJ IDEA.
 * User: fkleedorfer
 * Date: 23.12.12
 * Time: 18:43
 * To change this template use File | Settings | File Templates.
 */
public abstract class LazySpringBeanAutowiringSupport extends SpringBeanAutowiringSupport{
    /**
     * Lazy wiring for spring-managed dependencies.
     * TODO: figure out the proper way to connect spring and jaxws/metro or switch to spring-webservices entirely
     */
    protected void wireDependenciesLazily() {
        if (!isWired()) processInjectionBasedOnCurrentContext(this);
    }

    protected abstract boolean isWired();
}
