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

import java.net.URI;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import won.protocol.model.parentaware.ParentAware;
import won.protocol.model.parentaware.VersionedEntity;

/**
 * User: fkleedorfer
 * Date: 30.10.12
 */
@Entity
@Table(name = "connection", indexes = {
        @Index(name = "IDX_CONNECTION_NEEDURI_REMOTENEEDURI", columnList = "needURI, remoteNeedURI"),
    },
    uniqueConstraints = {
            @UniqueConstraint(name = "IDX_CONNECTION_UNIQUE_EVENT_CONTAINER_ID", columnNames = "event_container_id"),
            @UniqueConstraint(name = "IDX_CONNECTION_UNIQUE_DATASETHOLDER_ID", columnNames = "datasetholder_id"),
            @UniqueConstraint(name = "IDX_UNIQUE_CONNECTION", columnNames = { "needURI", "remoteNeedURI", "facetURI", "remoteFacetURI"})
    })
public class Connection implements ParentAware<ConnectionContainer>, VersionedEntity {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
  private int version = 0;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private Date lastUpdate = new Date();

  /* The public URI of this connection */
  @Column(name = "connectionURI", unique = true)
  @Convert(converter = URIConverter.class)
  private URI connectionURI;

  /* The uri of the connection's need object */
  @Column(name = "needURI")
  @Convert(converter = URIConverter.class)
  private URI needURI;

  /* The uri of the facet's type */
  @Column(name = "typeURI")
  @Convert(converter = URIConverter.class)
  private URI typeURI;
  
  /* The uri of the facet. This must be a resource defined in the need's content. The type of that resource is the typeURI*/
  @Column(name = "facetURI")
  @Convert(converter = URIConverter.class)
  private URI facetURI;
  
  /* The uri of the remote facet. This must be a resource defined in the remote need's content or null, if we don't know it yet */
  @Column(name = "remoteFacetURI")
  @Convert(converter = URIConverter.class)
  private URI remoteFacetURI;

  /* The URI of the remote connection */
  /* Caution: on the owner side, the remote connection URI is never known. */
  @Column(name = "remoteConnectionURI")
  @Convert(converter = URIConverter.class)
  private URI remoteConnectionURI;

  /* The URI of the remote need */
  @Column(name = "remoteNeedURI")
  @Convert(converter = URIConverter.class)
  private URI remoteNeedURI;

  /* The state of the connection */
  @Column(name = "state")
  @Enumerated(EnumType.STRING)
  private ConnectionState state;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_need_id")
  private ConnectionContainer parent;

  @OneToOne(fetch = FetchType.LAZY)
  private DatasetHolder datasetHolder;

  @JoinColumn(name = "event_container_id")
  @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL,
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
  @PreUpdate
  @PrePersist
  public void incrementVersion() {
    this.version++;
    this.lastUpdate = new Date();
  }

  //TODO: we may want to introduce a creation date?

  
  

    public URI getTypeURI() {
        return typeURI;
    }

    @Override
    public String toString() {
        return "Connection [id=" + id + ", connectionURI=" + connectionURI + ", needURI=" + needURI + ", typeURI="
                + typeURI + ", facetURI=" + facetURI + ", remoteFacetURI=" + remoteFacetURI + ", remoteConnectionURI="
                + remoteConnectionURI + ", remoteNeedURI=" + remoteNeedURI + ", state=" + state + "]";
    }

    public void setTypeURI(URI typeURI) {
        this.typeURI = typeURI;
    }
    
    public URI getFacetURI() {
        return facetURI;
    }

    public void setFacetURI(URI facetURI) {
        this.facetURI = facetURI;
    }    

    public URI getRemoteFacetURI() {
        return remoteFacetURI;
    }

    public void setRemoteFacetURI(URI remoteFacetURI) {
        this.remoteFacetURI = remoteFacetURI;
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

  protected void setVersion(final int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  protected void setLastUpdate(final Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public DatasetHolder getDatasetHolder() {
    return datasetHolder;
  }

  public void setDatasetHolder(final DatasetHolder datasetHolder) {
    this.datasetHolder = datasetHolder;
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
