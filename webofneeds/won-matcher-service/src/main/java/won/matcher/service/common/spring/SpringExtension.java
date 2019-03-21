package won.matcher.service.common.spring;

import org.springframework.context.ApplicationContext;

import akka.actor.AbstractExtensionId;
import akka.actor.Actor;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.Props;
import akka.routing.FromConfig;

/**
 * An Akka Extension to provide access to Spring managed Actor Beans.
 */
public class SpringExtension extends
  AbstractExtensionId<SpringExtension.SpringExt> {

  /**
   * The identifier used to access the SpringExtension.
   */
  public static SpringExtension SpringExtProvider = new SpringExtension();

  /**
   * Is used by Akka to instantiate the Extension identified by this
   * ExtensionId, internal use only.
   */
  @Override
  public SpringExt createExtension(ExtendedActorSystem system) {
    return new SpringExt();
  }

  /**
   * The Extension implementation.
   */
  public static class SpringExt implements Extension {
    private volatile ApplicationContext applicationContext;

    /**
     * Used to initialize the Spring application context for the extension.
     * @param applicationContext
     */
    public void initialize(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    /**
     * Create a Props for the specified actorClass using the
     * SpringActorProducer class.
     *
     * @param actorClass  class of an actor
     * @return a Props that will create the named actor bean using Spring
     */
    public Props props(final Class<? extends Actor> actorClass) {
      return Props.create(SpringActorProducer.class, applicationContext, actorClass);
    }

    /**
     * Create a Props for the specified actorClass and additional parameters using the
     * SpringActorProducer class.
     *
     * @param actorClass  class of an actor
     * @param params additional parameters for actor creation
     * @return a Props that will create the named actor bean using Spring
     */
    public Props props(final Class<? extends Actor> actorClass, Object... params) {
      return Props.create(SpringActorProducer.class, applicationContext, actorClass, params);
    }

    /**
     * Create Props from the configuration file for the specified actorClass using the
     * SpringActorProducer class.
     *
     * @param actorClass  class of an actor
     * @return a Props that will create the named actor bean using Spring
     */
    public Props fromConfigProps(final Class<? extends Actor> actorClass) {
      return FromConfig.getInstance().props(Props.create(SpringActorProducer.class, applicationContext, actorClass));
    }

    /**
     * Create Props from the configuration file  for the specified actorClass and additional parameters using the
     * SpringActorProducer class.
     *
     * @param actorClass  class of an actor
     * @param params additional parameters for actor creation
     * @return a Props that will create the named actor bean using Spring
     */
    public Props fromConfigProps(final Class<? extends Actor> actorClass, Object... params) {
      return FromConfig.getInstance().props(
        Props.create(SpringActorProducer.class, applicationContext, actorClass, params));
    }
  }
}
