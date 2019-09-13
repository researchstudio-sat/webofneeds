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

import won.protocol.model.parentaware.VersionedEntity;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "atom", uniqueConstraints = {
                @UniqueConstraint(name = "IDX_ATOM_UNIQUE_MESSAGE_CONTAINER_ID", columnNames = "message_container_id"),
                @UniqueConstraint(name = "IDX_ATOM_UNIQUE_DATASETHOLDER_ID", columnNames = "datatsetholder_id") })
// @Inheritance(strategy=InheritanceType.JOINED)
public class Atom implements VersionedEntity {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
    @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
    private int version = 0;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Date lastUpdate;
    /* The URI of the atom */
    @Column(name = "atomURI", unique = true)
    @Convert(converter = URIConverter.class)
    protected URI atomURI;
    /* The state of the atom */
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private AtomState state;
    /* The owner protocol endpoint URI where the owner of the atom can be reached */
    @Column(name = "ownerURI")
    @Convert(converter = URIConverter.class)
    private URI ownerURI;
    /*
     * The atom protocol endpoint URI where the won node of the atom can be reached
     */
    @Column(name = "wonNodeURI")
    @Convert(converter = URIConverter.class)
    private URI wonNodeURI;
    /* The creation date of the atom */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creationDate", nullable = false)
    private Date creationDate;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private DatasetHolder datatsetHolder;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DatasetHolder> attachmentDatasetHolders;
    // EAGERly loaded because accessed outside hibernate session in
    // OwnerProtocolCamelConfiguratorImpl TODO: change this!
    @ManyToMany(targetEntity = OwnerApplication.class, fetch = FetchType.EAGER)
    @JoinTable(name = "ATOM_OWNERAPP", joinColumns = @JoinColumn(name = "atom_id"), inverseJoinColumns = @JoinColumn(name = "owner_application_id"), uniqueConstraints = {
                    @UniqueConstraint(name = "IDX_NO_UNIQUE_ATOM_ID_OWNER_APPLICATION_ID", columnNames = { "atom_id",
                                    "owner_application_id" }) }, indexes = {
                                                    @Index(name = "IDX_NO_ATOM_ID", columnList = "atom_id") })
    private List<OwnerApplication> authorizedApplications;
    @JoinColumn(name = "message_container_id")
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    private AtomMessageContainer messageContainer;
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "atom", cascade = CascadeType.ALL, orphanRemoval = true)
    private ConnectionContainer connectionContainer;

    public AtomMessageContainer getMessageContainer() {
        return messageContainer;
    }

    @PreUpdate
    public void incrementVersion() {
        this.version++;
        if (this.state != AtomState.DELETED) {
            this.lastUpdate = new Date();
        }
    }

    @Override
    public Date getLastUpdate() {
        return lastUpdate;
    }

    protected void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    protected void setVersion(final int version) {
        this.version = version;
    }

    public void setMessageContainer(final AtomMessageContainer messageContainer) {
        this.messageContainer = messageContainer;
    }

    public void setConnectionContainer(final ConnectionContainer connectionContainer) {
        this.connectionContainer = connectionContainer;
    }

    public int getVersion() {
        return version;
    }

    public ConnectionContainer getConnectionContainer() {
        return connectionContainer;
    }

    @PrePersist
    protected void onCreate() {
        creationDate = new Date();
        lastUpdate = creationDate;
        incrementVersion();
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
        lastUpdate = creationDate;
    }

    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public URI getAtomURI() {
        return atomURI;
    }

    public void setAtomURI(final URI URI) {
        this.atomURI = URI;
    }

    public AtomState getState() {
        return state;
    }

    public void setState(final AtomState state) {
        this.state = state;
    }

    public URI getOwnerURI() {
        return ownerURI;
    }

    public void setOwnerURI(final URI ownerURI) {
        this.ownerURI = ownerURI;
    }

    public DatasetHolder getDatatsetHolder() {
        return datatsetHolder;
    }

    public void setDatatsetHolder(final DatasetHolder datatsetHolder) {
        this.datatsetHolder = datatsetHolder;
    }

    public List<DatasetHolder> getAttachmentDatasetHolders() {
        return attachmentDatasetHolders;
    }

    public void setAttachmentDatasetHolders(final List<DatasetHolder> attachmentDatasetHolders) {
        this.attachmentDatasetHolders = attachmentDatasetHolders;
    }

    @Override
    public String toString() {
        return "Atom{" + "id=" + id + ", atomURI=" + atomURI + ", state=" + state + ", ownerURI=" + ownerURI
                        + ", creationDate=" + creationDate + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Atom))
            return false;
        final Atom atom = (Atom) o;
        if (!Objects.equals(atomURI, atom.atomURI))
            return false;
        if (!Objects.equals(ownerURI, atom.ownerURI))
            return false;
        if (!Objects.equals(creationDate, atom.creationDate))
            return false;
        return state == atom.state;
    }

    @Override
    public int hashCode() {
        int result = atomURI.hashCode();
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + ownerURI.hashCode();
        result = 31 * result + creationDate.hashCode();
        return result;
    }

    public void resetAllAtomData() {
        this.attachmentDatasetHolders = null;
        this.authorizedApplications = null;
        this.connectionContainer = null;
        this.creationDate = new Date(0);
        this.lastUpdate = new Date(0);
        // this.messageContainer = null;
        this.ownerURI = null;
        // this.wonNodeURI = null;
    }

    public List<OwnerApplication> getAuthorizedApplications() {
        return authorizedApplications;
    }

    public void setAuthorizedApplications(List<OwnerApplication> authorizedApplications) {
        this.authorizedApplications = authorizedApplications;
    }

    public URI getWonNodeURI() {
        return wonNodeURI;
    }

    public void setWonNodeURI(URI wonNodeURI) {
        this.wonNodeURI = wonNodeURI;
    }
}
