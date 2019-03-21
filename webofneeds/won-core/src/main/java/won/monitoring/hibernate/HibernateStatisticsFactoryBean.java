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

package won.monitoring.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Exports an MBean that makes hibernate statistics accessible.
 */
@Component
public class HibernateStatisticsFactoryBean implements FactoryBean<Statistics> {
  @Autowired
  private SessionFactory sessionFactory;

  @Override
  public Statistics getObject() throws Exception {
    return this.sessionFactory.getStatistics();
  }

  @Override
  public Class<?> getObjectType() {
    return Statistics.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public void setSessionFactory(final SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }
}
