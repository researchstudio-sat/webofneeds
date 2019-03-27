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
package won.owner.model;

import java.net.URI;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import won.protocol.model.NeedState;
import won.protocol.model.URIConverter;

/**
 * Entity wrapping a uri identifying a user's need.
 */
@Entity
public class UserNeed {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
    @Column(name = "uri", unique = true)
    @Convert(converter = URIConverter.class)
    private URI uri;
    @Column(name = "matches")
    private boolean matches;
    @Column(name = "requests")
    private boolean requests = true;
    @Column(name = "conversations")
    private boolean conversations = true;
    /* The creation date of the (as observed by the owner app) */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creationDate", nullable = false)
    private Date creationDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private NeedState state;

    public UserNeed() {
    }

    @PrePersist
    protected void onCreate() {
        creationDate = new Date();
        state = NeedState.ACTIVE;
    }

    public UserNeed(URI uri) {
        this.uri = uri;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public boolean isMatches() {
        return matches;
    }

    public void setMatches(final boolean matches) {
        this.matches = matches;
    }

    public boolean isRequests() {
        return requests;
    }

    public void setRequests(final boolean requests) {
        this.requests = requests;
    }

    public boolean isConversations() {
        return conversations;
    }

    public void setConversations(final boolean conversations) {
        this.conversations = conversations;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    public NeedState getState() {
        return state;
    }

    public void setState(NeedState state) {
        this.state = state;
    }
}
