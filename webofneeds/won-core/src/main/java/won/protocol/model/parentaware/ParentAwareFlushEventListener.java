/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.model.parentaware;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParentAwareFlushEventListener implements FlushEntityEventListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static final ParentAwareFlushEventListener INSTANCE = new ParentAwareFlushEventListener();

    @Override
    public void onFlushEntity(final FlushEntityEvent event) throws HibernateException {
        final EntityEntry entry = event.getEntityEntry();
        final Object entity = event.getEntity();
        final boolean mightBeDirty = entry.requiresDirtyCheck(entity);
        if (mightBeDirty && entity instanceof ParentAware) {
            ParentAware parentAware = (ParentAware) entity;
            if (updated(event)) {
                VersionedEntity parent = parentAware.getParent();
                if (parent == null)
                    return;
                if (logger.isDebugEnabled()) {
                    logger.debug("Incrementing {} entity version because a {} child entity has been updated", parent,
                                    entity);
                }
                if (!(parent instanceof HibernateProxy)) {
                    // we have to do the increment manually
                    parent.incrementVersion();
                }
                Hibernate.initialize(parent);
                event.getSession().save(parent);
            }
        }
    }

    private boolean deleted(FlushEntityEvent event) {
        return event.getEntityEntry().getStatus() == Status.DELETED;
    }

    private boolean updated(FlushEntityEvent event) {
        final EntityEntry entry = event.getEntityEntry();
        final Object entity = event.getEntity();
        int[] dirtyProperties;
        EntityPersister persister = entry.getPersister();
        final Object[] values = event.getPropertyValues();
        SessionImplementor session = event.getSession();
        if (event.hasDatabaseSnapshot()) {
            dirtyProperties = persister.findModified(event.getDatabaseSnapshot(), values, entity, session);
        } else {
            dirtyProperties = persister.findDirty(values, entry.getLoadedState(), entity, session);
        }
        return dirtyProperties != null;
    }
}
