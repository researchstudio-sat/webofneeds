/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.model.parentaware;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integrator for hibernate to allow listening to entity changes and update
 * parent entity versions.
 *
 * Modeled after this example:
 * https://vladmihalcea.com/2016/08/30/how-to-increment-the-parent-entity-version-whenever-a-child-entity-gets-modified-with-jpa-and-hibernate/
 */
public class ParentAwareEventListenerIntegrator implements Integrator {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public ParentAwareEventListenerIntegrator() {
  }

  @Override
  public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
    // Do nothing
  }

  @Override
  public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor,
      SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {
    logger.debug("integrating listeners for ParentAware entities");
    final EventListenerRegistry eventListenerRegistry = sessionFactoryServiceRegistry
        .getService(EventListenerRegistry.class);

    eventListenerRegistry.appendListeners(EventType.PERSIST, ParentAwarePersistEventListener.INSTANCE);
    eventListenerRegistry.appendListeners(EventType.FLUSH_ENTITY, ParentAwareFlushEventListener.INSTANCE);
  }

}
