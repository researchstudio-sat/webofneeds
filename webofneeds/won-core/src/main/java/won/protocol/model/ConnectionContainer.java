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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import won.protocol.model.parentaware.ParentAware;
import won.protocol.model.parentaware.VersionedEntity;

@Entity
@Table(name = "connection_container")
public class ConnectionContainer implements ParentAware<Need>, VersionedEntity {
  @Id
  @Column(name = "id")
  protected Long id;

  @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
  private int version = 0;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private Date lastUpdate = new Date();

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "need_id")
  @MapsId
  private Need need;

  @Override
  @PrePersist
  @PreUpdate
  public void incrementVersion() {
    this.version++;
    this.lastUpdate = new Date();
  }

  @Override
  public Date getLastUpdate() {
    return lastUpdate;
  }

  public int getVersion() {
    return version;
  }

  public Need getNeed() {
    return need;
  }

  @Override
  public Need getParent() {
    return getNeed();
  }

  public void setNeed(final Need need) {
    this.need = need;
  }

  public Long getId() {
    return this.id;
  }

  public ConnectionContainer(final Need need) {
    this.need = need;
    if (need != null) {
      need.setConnectionContainer(this);
    }
  }

  public ConnectionContainer() {
  }
}
