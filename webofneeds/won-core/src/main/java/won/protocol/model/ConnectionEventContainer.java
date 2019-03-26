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

package won.protocol.model;

import java.net.URI;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

import won.protocol.model.parentaware.ParentAware;

@Entity
@DiscriminatorValue("Connection")
public class ConnectionEventContainer extends EventContainer implements ParentAware<Connection> {
  @OneToOne(fetch = FetchType.LAZY, mappedBy = "eventContainer", optional = false)
  private Connection connection;

  public ConnectionEventContainer() {
  }

  public ConnectionEventContainer(final Connection connection, URI parentUri) {
    super(parentUri);
    this.connection = connection;
    if (connection != null) {
      connection.setEventContainer(this);
    }
  }

  public Connection getConnection() {
    return connection;
  }

  @Override
  public Connection getParent() {
    return getConnection();
  }

  protected void setConnection(final Connection connection) {
    this.connection = connection;
  }
}
