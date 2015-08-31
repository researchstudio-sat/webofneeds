package common.spring;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.springframework.context.ApplicationContext;

/**
 * An actor producer that lets Spring create the Actor instances.
 */
public final class SpringActorProducer implements IndirectActorProducer {
  private final ApplicationContext applicationContext;
  private final Class<? extends Actor> actorClass;

  public SpringActorProducer(final ApplicationContext applicationContext, final Class<? extends Actor> actorClass) {
    this.applicationContext = applicationContext;
    this.actorClass = actorClass;
  }

  @Override
  public Actor produce() {
    return applicationContext.getBean(actorClass);
  }

  @Override
  public Class<? extends Actor> actorClass() {
    return actorClass;
  }
}
