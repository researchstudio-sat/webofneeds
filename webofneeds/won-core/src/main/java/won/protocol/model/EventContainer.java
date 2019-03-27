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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import won.protocol.model.parentaware.VersionedEntity;

@Entity
@Inheritance
@DiscriminatorColumn(name = "parent_type")
@Table(name = "event_container")
public abstract class EventContainer implements VersionedEntity {
    @Id
    @GeneratedValue
    @Column(name = "id")
    protected Long id;
    @Column(name = "parent_uri", nullable = false, unique = true, updatable = false)
    @Convert(converter = URIConverter.class)
    private URI parentUri;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "eventContainer")
    private Collection<MessageEventPlaceholder> events = new ArrayList<>(1);
    @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
    private int version = 0;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Date lastUpdate = new Date();

    public EventContainer() {
    }

    public EventContainer(final URI parentUri) {
        this.parentUri = parentUri;
    }

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

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Collection<MessageEventPlaceholder> getEvents() {
        return events;
    }

    public void setEvents(final Collection<MessageEventPlaceholder> events) {
        this.events = events;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    public URI getParentUri() {
        return parentUri;
    }

    public void setParentUri(final URI parentUri) {
        this.parentUri = parentUri;
    }
}
