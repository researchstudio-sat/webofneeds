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


import javax.persistence.*;
import java.net.URI;

@Entity
@Table(name = "match",
    uniqueConstraints=@UniqueConstraint(columnNames={"fromNeed", "toNeed","originator"})
)
public class Match
{
    /* This is the event ID to the corresponding match */
    @Id
    @GeneratedValue
    @Column( name = "id")
    private long id;
    @Column( name = "fromNeed")
    private URI fromNeed;
    @Column( name = "toNeed")
    private URI toNeed;
    //TODO: there's no need for score to be a double. short should be sufficient
    @Column( name = "score")
    private double score;
    @Column( name = "originator")
    private URI originator;
    //TODO: set unique after this filed is filled with information
    @Column( name = "eventId")
    private long eventId;

  @Override
  public String toString()
  {
    return "Match{" +
        ", id=" + id +
        ", fromNeed=" + fromNeed +
        ", toNeed=" + toNeed +
        ", score=" + score +
        ", originator=" + originator +
        ", eventId=" + eventId +
        '}';
  }

    // TODO: we rely on the id being marshalled in the current owner implementation, but this is not neccessary
    // it would be better to make this field @XmlTransient

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public URI getFromNeed() {
        return fromNeed;
    }

    public void setFromNeed(URI fromNeed) {
        this.fromNeed = fromNeed;
    }

    public URI getToNeed() {
        return toNeed;
    }

    public void setToNeed(URI toNeed) {
        this.toNeed = toNeed;
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
