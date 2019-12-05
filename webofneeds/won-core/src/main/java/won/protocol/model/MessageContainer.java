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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import won.protocol.WonConstants;
import won.protocol.model.parentaware.VersionedEntity;

@Entity
@Inheritance
@DiscriminatorColumn(name = "parent_type")
@Table(name = "message_container", indexes = {
                @Index(name = "IDX_PARENT_URI", columnList = "parent_uri") }, uniqueConstraints = {
                                @UniqueConstraint(name = "IDX_UNIQUE_PARENT_URI", columnNames = { "parent_uri" })
                })
public abstract class MessageContainer implements VersionedEntity {
    @Id
    @GeneratedValue
    @Column(name = "id")
    protected Long id;
    @Column(name = "parent_uri", nullable = false, unique = true, updatable = false)
    @Convert(converter = URIConverter.class)
    private URI parentUri;
    @Convert(converter = URICountSetConverter.class)
    private Set<URICount> unconfirmed = new HashSet<>();
    @Convert(converter = URIMapToURISetConverter.class)
    private Map<URI, Set<URI>> pendingConfirmations = new HashMap<>();
    @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
    private int version = 0;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Date lastUpdate = new Date();

    public MessageContainer() {
    }

    public MessageContainer(final URI parentUri) {
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

    /**
     * Returns the set of unconfirmed message URIs, increments the counter for each
     * URI and removes the entries whose counts are now greater than
     * <code>WonConstants.MAX_CONFIRMATIONS</code>.
     * 
     * @return the unconfirmed message URIs
     */
    public synchronized Set<URI> getUnconfirmedAndIncrementAndCleanup() {
        Set<URI> result = unconfirmed.stream().map(uc -> uc.getUri()).collect(Collectors.toSet());
        unconfirmed = unconfirmed.stream()
                        .filter(uc -> uc.getCount() < WonConstants.MAX_CONFIRMATIONS)
                        .map(uc -> uc.increment())
                        .collect(Collectors.toSet());
        return result;
    }

    /**
     * Get the list of unconfirmed messages WITHOUT affecting counters. Should only
     * be used for diagnostic purposes.
     * 
     * @return
     */
    public synchronized Set<URICount> peekAtUnconfirmed() {
        return Collections.unmodifiableSet(unconfirmed);
    }

    public int getUnconfirmedCount() {
        return unconfirmed.size();
    }

    public synchronized void addUnconfirmed(URI toAdd) {
        Objects.requireNonNull(toAdd);
        // will not cause duplicates because URICount.equals only looks at the URI
        if (!unconfirmed.stream().anyMatch(uc -> toAdd.equals(uc.getUri()))) {
            unconfirmed.add(new URICount(toAdd, 0));
        }
    }

    public synchronized void removeUnconfirmed(URI toRemove) {
        Iterator<URICount> it = unconfirmed.iterator();
        while (it.hasNext()) {
            URICount uc = it.next();
            if (Objects.equals(uc.getUri(), toRemove)) {
                it.remove();
            }
        }
    }

    public synchronized void removeUnconfirmed(Collection<URI> toRemove) {
        Iterator<URICount> it = unconfirmed.iterator();
        while (it.hasNext()) {
            URICount uc = it.next();
            if (toRemove.contains(uc.getUri()))
                it.remove();
        }
    }

    public synchronized Map<URI, Set<URI>> getPendingConfirmations() {
        return Collections.unmodifiableMap(pendingConfirmations);
    }

    public synchronized void removePendingConfirmations(Collection<URI> toDelete) {
        toDelete.forEach(uri -> pendingConfirmations.remove(uri));
    }

    public void addPendingConfirmation(URI toBeConfirmed, Set<URI> transitiveConfirmations) {
        pendingConfirmations.put(toBeConfirmed, transitiveConfirmations);
    }
}
