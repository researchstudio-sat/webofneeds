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
import java.util.List;

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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlTransient;

import won.protocol.model.parentaware.VersionedEntity;

/**
 *
 */
@Entity
@Table(name = "need", uniqueConstraints = {
    @UniqueConstraint(name = "IDX_NEED_UNIQUE_EVENT_CONTAINER_ID", columnNames = "event_container_id"),
    @UniqueConstraint(name = "IDX_NEED_UNIQUE_DATASETHOLDER_ID", columnNames = "datatsetholder_id") })
//@Inheritance(strategy=InheritanceType.JOINED)
public class Need implements VersionedEntity {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
  private int version = 0;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private Date lastUpdate;

  /* The URI of the need */
  @Column(name = "needURI", unique = true)
  @Convert(converter = URIConverter.class)
  protected URI needURI;

  /* The state of the need */
  @Column(name = "state")
  @Enumerated(EnumType.STRING)
  private NeedState state;

  /* The owner protocol endpoint URI where the owner of the need can be reached */
  @Column(name = "ownerURI")
  @Convert(converter = URIConverter.class)
  private URI ownerURI;

  /*
   * The need protocol endpoint URI where the won node of the need can be reached
   */
  @Column(name = "wonNodeURI")
  @Convert(converter = URIConverter.class)
  private URI wonNodeURI;

  /* The creation date of the need */
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
  @JoinTable(name = "NEED_OWNERAPP", joinColumns = @JoinColumn(name = "need_id"), inverseJoinColumns = @JoinColumn(name = "owner_application_id"), uniqueConstraints = {
      @UniqueConstraint(name = "IDX_NO_UNIQUE_NEED_ID_OWNER_APPLICATION_ID", columnNames = { "need_id",
          "owner_application_id" }) }, indexes = { @Index(name = "IDX_NO_NEED_ID", columnList = "need_id") })
  private List<OwnerApplication> authorizedApplications;

  @JoinColumn(name = "event_container_id")
  @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
  private NeedEventContainer eventContainer;

  @OneToOne(fetch = FetchType.LAZY, mappedBy = "need", cascade = CascadeType.ALL, orphanRemoval = true)
  private ConnectionContainer connectionContainer;

  public NeedEventContainer getEventContainer() {
    return eventContainer;
  }

  @PreUpdate
  public void incrementVersion() {
    this.version++;
    if (this.state != NeedState.DELETED) {
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

  public void setEventContainer(final NeedEventContainer eventContainer) {
    this.eventContainer = eventContainer;
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

  public URI getNeedURI() {
    return needURI;
  }

  public void setNeedURI(final URI URI) {
    this.needURI = URI;
  }

  public NeedState getState() {
    return state;
  }

  public void setState(final NeedState state) {
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
    return "Need{" + "id=" + id + ", needURI=" + needURI + ", state=" + state + ", ownerURI=" + ownerURI
        + ", creationDate=" + creationDate + '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Need))
      return false;

    final Need need = (Need) o;

    if (needURI != null ? !needURI.equals(need.needURI) : need.needURI != null)
      return false;
    if (ownerURI != null ? !ownerURI.equals(need.ownerURI) : need.ownerURI != null)
      return false;
    if (creationDate != null ? !creationDate.equals(need.creationDate) : need.creationDate != null)
      return false;
    if (state != need.state)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = needURI.hashCode();
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + ownerURI.hashCode();
    result = 31 * result + creationDate.hashCode();
    return result;
  }

  public void resetAllNeedData() {
    this.attachmentDatasetHolders = null;
    this.authorizedApplications = null;
    this.connectionContainer = null;
    this.creationDate = new Date(0);
    this.lastUpdate = new Date(0);
    // this.eventContainer = null;
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
