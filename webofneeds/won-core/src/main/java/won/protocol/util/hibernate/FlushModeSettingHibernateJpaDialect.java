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

package won.protocol.util.hibernate;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

/**
 *
 * JPA dialect that allows us to set the hibernate session flush mode.
 * Was used to check if the flushmode has something to do with the hibernate-managed persistence manager
 * filling up with entities. Turned out it was just a @ManyToMany relation that was set to fetch mode EAGER.
 *
 * Taken from http://forum.spring.io/forum/spring-projects/data/48262-jpa-configuring-hibernate-flushmode
 *
 */
public class FlushModeSettingHibernateJpaDialect extends HibernateJpaDialect
{
  private FlushMode flushMode;

  public String getFlushMode() {
    return flushMode!=null ? flushMode.toString() : null;
  }

  public void setFlushMode(String aFlushMode) {
    flushMode = FlushMode.valueOf(aFlushMode);
    if (aFlushMode != null && flushMode == null) {
      throw new IllegalArgumentException (aFlushMode+" value invalid. See class org.hibernate.FlushMode for valid values");
    }
  }

  public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
    throws PersistenceException {

    Session session = getSession(entityManager);
    FlushMode currentFlushMode = session.getFlushMode();
    FlushMode previousFlushMode = null;
    if (getFlushMode() != null) {
      session.setFlushMode(flushMode);
      previousFlushMode = currentFlushMode;
    } else if (readOnly) {
      // We should suppress flushing for a read-only transaction.
      session.setFlushMode(FlushMode.MANUAL);
      previousFlushMode = currentFlushMode;
    }
    else {
      // We need AUTO or COMMIT for a non-read-only transaction.
      if (currentFlushMode.lessThan(FlushMode.COMMIT)) {
        session.setFlushMode(FlushMode.AUTO);
        previousFlushMode = currentFlushMode;
      }
    }
    return new SessionTransactionData(session, previousFlushMode);
  }

  public void cleanupTransaction(Object transactionData) {
    ((SessionTransactionData) transactionData).resetFlushMode();
  }

  private static class SessionTransactionData {

    private final Session session;

    private final FlushMode previousFlushMode;

    public SessionTransactionData(Session session, FlushMode previousFlushMode) {
      this.session = session;
      this.previousFlushMode = previousFlushMode;
    }

    public void resetFlushMode() {
      if (this.previousFlushMode != null) {
        this.session.setFlushMode(this.previousFlushMode);
      }
    }

    public void clearSession(){
      this.session.clear();
    }
  }
}
