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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "match", uniqueConstraints = @UniqueConstraint(columnNames = { "fromAtom", "toAtom", "originator" }))
public class Match {
    /* This is the event ID to the corresponding match */
    @Id
    @GeneratedValue
    @Column(name = "id")
    private long id;
    @Column(name = "fromAtom")
    @Convert(converter = URIConverter.class)
    private URI fromAtom;
    @Column(name = "toAtom")
    @Convert(converter = URIConverter.class)
    private URI toAtom;
    @Column(name = "score")
    private double score;
    @Column(name = "originator")
    @Convert(converter = URIConverter.class)
    private URI originator;
    @Column(name = "eventId")
    private long eventId;

    @Override
    public String toString() {
        return "Match{" + ", id=" + id + ", fromAtom=" + fromAtom + ", toAtom=" + toAtom + ", score=" + score
                        + ", originator=" + originator + ", eventId=" + eventId + '}';
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public URI getFromAtom() {
        return fromAtom;
    }

    public void setFromAtom(URI fromAtom) {
        this.fromAtom = fromAtom;
    }

    public URI getToAtom() {
        return toAtom;
    }

    public void setToAtom(URI toAtom) {
        this.toAtom = toAtom;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public URI getOriginator() {
        return originator;
    }

    public void setOriginator(URI originator) {
        this.originator = originator;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }
}
