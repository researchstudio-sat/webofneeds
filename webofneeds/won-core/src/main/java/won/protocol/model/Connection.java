/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.model;

import won.protocol.model.parentaware.ParentAware;

import javax.persistence.*;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 30.10.12
 */
@Entity
@Table(name = "connection", indexes = { @Index(name = "IDX_CONNECTION_NEEDURI_REMOTENEEDURI", columnList = "needURI, " +
  "remoteNeedURI")}, uniqueConstraints = {@UniqueConstraint(name="IDX_UNIQUE_CONNECTION", columnNames = {"needURI", "remoteNeedURI", "typeURI"})})
public class Connection implements ParentAware<ConnectionContainer>
{
  @Id
  @GeneratedValue
  @Column( name = "id" )
  private Long id;

  @Version
  @Column(name="version", columnDefinition = "integer DEFAULT 0", nullable = false)
  private long version = 0L;

  /* The public URI of this connection */
  @Column( name = "connectionURI", unique = true)
  @Convert( converter = URIConverter.class )
  private URI connectionURI;

  /* The uri of the connection's need object */
  @Column( name = "needURI")
  @Convert( converter = URIConverter.class )
  private URI needURI;

  /* The uri of the facet's type */
  @Column( name = "typeURI")
  @Convert( converter = URIConverter.class )
  private URI typeURI;

  /* The URI of the remote connection */
  /* Caution: on the owner side, the remote connection URI is never known. */
  @Column( name = "remoteConnectionURI")
  @Convert( converter = URIConverter.class )
  private URI remoteConnectionURI;

  /* The URI of the remote need */
  @Column( name = "remoteNeedURI")
  @Convert( converter = URIConverter.class )
  private URI remoteNeedURI;

  /* The state of the connection */
  @Column( name = "state")
  @Enumerated ( EnumType.STRING )
  private ConnectionState state;

  @ManyToOne(fetch = FetchType.LAZY)
  private ConnectionContainer parent;

  @OneToOne (fetch = FetchType.LAZY, mappedBy="connection", optional = true, cascade = CascadeType.ALL,
    orphanRemoval = true)
  private ConnectionEventContainer eventContainer = null;

  @Override
  public ConnectionContainer getParent() {
    return this.parent;
  }


  public ConnectionEventContainer getEventContainer() {
    return eventContainer;
  }

  public void setEventContainer(final ConnectionEventContainer eventContainer) {
    this.eventContainer = eventContainer;
  }

  public void setParent(final ConnectionContainer parent) {
    this.parent = parent;
  }

  @Override
  public String toString()
  {
    return "Connection{" +
        "id=" + id +
        ", connectionURI=" + connectionURI +
        ", needURI=" + needURI +
        ", remoteConnectionURI=" + remoteConnectionURI +
        ", remoteNeedURI=" + remoteNeedURI +
        ", state=" + state +
        '}';
  }

    public URI getTypeURI() {
        return typeURI;
    }

    public void setTypeURI(URI typeURI) {
        this.typeURI = typeURI;
    }

    public Long getId() {
      return id;
  }

  public void setId(Long id) {
      this.id = id;
  }

  public URI getConnectionURI()
  {
    return connectionURI;
  }

  public void setConnectionURI(final URI connectionURI)
  {
    this.connectionURI = connectionURI;
  }

  public URI getNeedURI()
  {
    return needURI;
  }

  public void setNeedURI(final URI needURI)
  {
    this.needURI = needURI;
  }

  public URI getRemoteConnectionURI()
  {
    return remoteConnectionURI;
  }

  public void setRemoteConnectionURI(final URI remoteConnectionURI)
  {
    this.remoteConnectionURI = remoteConnectionURI;
  }

  public URI getRemoteNeedURI()
  {
    return remoteNeedURI;
  }

  public void setRemoteNeedURI(final URI remoteNeedURI)
  {
    this.remoteNeedURI = remoteNeedURI;
  }

  public ConnectionState getState()
  {
    return state;
  }

  public void setState(final ConnectionState state)
  {
    this.state = state;
  }

  protected void setVersion(final long version) {
    this.version = version;
  }

  public long getVersion() {
    return version;
  }




  @Override
  public boolean equals(final Object o)
  {
    if (this == o) return true;
    if (!(o instanceof Connection)) return false;

    final Connection that = (Connection) o;

    if (connectionURI != null ? !connectionURI.equals(that.connectionURI) : that.connectionURI != null) return false;
    if (needURI != null ? !needURI.equals(that.needURI) : that.needURI != null) return false;
    if (remoteConnectionURI != null ? !remoteConnectionURI.equals(that.remoteConnectionURI) : that.remoteConnectionURI != null)
      return false;
    if (remoteNeedURI != null ? !remoteNeedURI.equals(that.remoteNeedURI) : that.remoteNeedURI != null) return false;
    if (state != that.state) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = connectionURI != null ? connectionURI.hashCode() : 0;
    result = 31 * result + (needURI != null ? needURI.hashCode() : 0);
    result = 31 * result + (remoteConnectionURI != null ? remoteConnectionURI.hashCode() : 0);
    result = 31 * result + (remoteNeedURI != null ? remoteNeedURI.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    return result;
  }
}
