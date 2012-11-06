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
@Table(name = "match")
public class Match
{
    @Id
    @GeneratedValue
    @Column( name = "id" )
    private Long id;
    @Column( name = "fromNeed")
    private URI fromNeed;
    @Column( name = "toNeed")
    private URI toNeed;
    @Column( name = "score")
    private double score;
    @Column( name = "originator")
    private URI originator;

  @Override
  public String toString()
  {
    return "Match{" +
        "id=" + id +
        ", fromNeed=" + fromNeed +
        ", toNeed=" + toNeed +
        ", score=" + score +
        ", originator=" + originator +
        '}';
  }

  public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
}
