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

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class ParentAwarePersistEventListener implements PersistEventListener
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  public static final ParentAwarePersistEventListener INSTANCE = new ParentAwarePersistEventListener();

  @Override
  public void onPersist(final PersistEvent event) throws HibernateException {
    final Object entity = event.getObject();

    if(entity instanceof ParentAware) {
      ParentAware rootAware = (ParentAware) entity;
      Object root = rootAware.getParent();
      if (root == null) return;
      event.getSession().buildLockRequest(new LockOptions().setLockMode(LockMode.OPTIMISTIC_FORCE_INCREMENT)).lock(root);
      if (logger.isDebugEnabled()) {
        logger.debug("Incrementing {} entity version because a {} child entity has been inserted", root, entity);
      }
    }
  }

  @Override
  public void onPersist(final PersistEvent event, final Map createdAlready) throws HibernateException {
    onPersist(event);
  }
}
