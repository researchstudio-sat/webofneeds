package won.matcher.service.common.spring;

import org.springframework.context.ApplicationContext;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;

/**
 * An actor producer that lets Spring create the Actor instances.
 */
public final class SpringActorProducer implements IndirectActorProducer {

  private final ApplicationContext applicationContext;
  private final Class<? extends Actor> actorClass;
  private final Object params[];

  public SpringActorProducer(final ApplicationContext applicationContext, final Class<? extends Actor> actorClass) {
    this.applicationContext = applicationContext;
    this.actorClass = actorClass;
    this.params = null;
  }

  public SpringActorProducer(final ApplicationContext applicationContext, final Class<? extends Actor> actorClass,
      Object... params) {
    this.applicationContext = applicationContext;
    this.actorClass = actorClass;
    this.params = params;
  }

  @Override
  public Actor produce() {

    if (params != null) {
      return applicationContext.getBean(actorClass, params);
    }
    return applicationContext.getBean(actorClass);
  }

  @Override
  public Class<? extends Actor> actorClass() {
    return actorClass;
  }
}
