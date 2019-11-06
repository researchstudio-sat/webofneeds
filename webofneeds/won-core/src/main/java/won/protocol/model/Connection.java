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
package won.protocol.model;

import java.net.URI;
import java.util.Date;
import java.util.Objects;

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
 * User: fkleedorfer Date: 30.10.12
 */
@Entity
@Table(name = "connection", indexes = {
                @Index(name = "IDX_CONNECTION_ATOMURI_TARGETATOMURI", columnList = "atomURI, targetAtomURI"), }, uniqueConstraints = {
                                @UniqueConstraint(name = "IDX_CONNECTION_UNIQUE_MESSAGE_CONTAINER_ID", columnNames = "message_container_id"),
                                @UniqueConstraint(name = "IDX_CONNECTION_UNIQUE_DATASETHOLDER_ID", columnNames = "datasetholder_id"),
                                @UniqueConstraint(name = "IDX_UNIQUE_CONNECTION", columnNames = { "atomURI",
                                                "targetAtomURI", "socketURI", "targetSocketURI" }) })
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
    /* The uri of the connection's atom object */
    @Column(name = "atomURI")
    @Convert(converter = URIConverter.class)
    private URI atomURI;
    /* The uri of the socket's type */
    @Column(name = "typeURI")
    @Convert(converter = URIConverter.class)
    private URI typeURI;
    /*
     * The uri of the socket. This must be a resource defined in the atom's content.
     * The type of that resource is the typeURI
     */
    @Column(name = "socketURI")
    @Convert(converter = URIConverter.class)
    private URI socketURI;
    /*
     * The uri of the remote socket. This must be a resource defined in the remote
     * atom's content or null, if we don't know it yet
     */
    @Column(name = "targetSocketURI")
    @Convert(converter = URIConverter.class)
    private URI targetSocketURI;
    /* The URI of the remote connection */
    /* Caution: on the owner side, the remote connection URI is never known. */
    @Column(name = "targetConnectionURI")
    @Convert(converter = URIConverter.class)
    private URI targetConnectionURI;
    /* The URI of the remote atom */
    @Column(name = "targetAtomURI")
    @Convert(converter = URIConverter.class)
    private URI targetAtomURI;
    /* The state of the connection */
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private ConnectionState state;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_atom_id")
    private ConnectionContainer parent;
    @OneToOne(fetch = FetchType.LAZY)
    private DatasetHolder datasetHolder;
    @JoinColumn(name = "message_container_id")
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    private ConnectionMessageContainer messageContainer = null;

    @Override
    public ConnectionContainer getParent() {
        return this.parent;
    }

    public ConnectionMessageContainer getMessageContainer() {
        return messageContainer;
    }

    public void setMessageContainer(final ConnectionMessageContainer messageContainer) {
        this.messageContainer = messageContainer;
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

    // TODO: we may want to introduce a creation date?
    public URI getTypeURI() {
        return typeURI;
    }

    @Override
    public String toString() {
        return "Connection [id=" + id + ", connectionURI=" + connectionURI + ", atomURI=" + atomURI + ", typeURI="
                        + typeURI + ", socketURI=" + socketURI + ", targetSocketURI=" + targetSocketURI
                        + ", targetConnectionURI=" + targetConnectionURI + ", targetAtomURI=" + targetAtomURI
                        + ", state=" + state + "]";
    }

    public void setTypeURI(URI typeURI) {
        this.typeURI = typeURI;
    }

    public URI getSocketURI() {
        return socketURI;
    }

    public void setSocketURI(URI socketURI) {
        this.socketURI = socketURI;
    }

    public URI getTargetSocketURI() {
        return targetSocketURI;
    }

    public void setTargetSocketURI(URI targetSocketURI) {
        this.targetSocketURI = targetSocketURI;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public URI getConnectionURI() {
        return connectionURI;
    }

    public void setConnectionURI(final URI connectionURI) {
        this.connectionURI = connectionURI;
    }

    public URI getAtomURI() {
        return atomURI;
    }

    public void setAtomURI(final URI atomURI) {
        this.atomURI = atomURI;
    }

    public URI getTargetConnectionURI() {
        return targetConnectionURI;
    }

    public void setTargetConnectionURI(final URI targetConnectionURI) {
        this.targetConnectionURI = targetConnectionURI;
    }

    public URI getTargetAtomURI() {
        return targetAtomURI;
    }

    public void setTargetAtomURI(final URI targetAtomURI) {
        this.targetAtomURI = targetAtomURI;
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(final ConnectionState state) {
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
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Connection))
            return false;
        final Connection that = (Connection) o;
        if (!Objects.equals(connectionURI, that.connectionURI))
            return false;
        if (!Objects.equals(atomURI, that.atomURI))
            return false;
        if (!Objects.equals(targetConnectionURI, that.targetConnectionURI))
            return false;
        if (!Objects.equals(targetAtomURI, that.targetAtomURI))
            return false;
        return state == that.state;
    }

    @Override
    public int hashCode() {
        int result = connectionURI != null ? connectionURI.hashCode() : 0;
        result = 31 * result + (atomURI != null ? atomURI.hashCode() : 0);
        result = 31 * result + (targetConnectionURI != null ? targetConnectionURI.hashCode() : 0);
        result = 31 * result + (targetAtomURI != null ? targetAtomURI.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        return result;
    }
}
