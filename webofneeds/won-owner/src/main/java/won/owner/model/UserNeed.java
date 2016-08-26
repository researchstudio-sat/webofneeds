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

package won.owner.model;

import won.protocol.model.URIConverter;

import javax.persistence.*;
import java.net.URI;

/**
 * Entity wrapping a uri identifying a user's need.
 */
@Entity
public class UserNeed
{
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column( name = "uri", unique = true)
  @Convert( converter = URIConverter.class)
  private URI uri;

  @Column( name = "matches")
  private boolean matches;

  @Column( name = "requests")
  private boolean requests = true;

  @Column( name = "conversations")
  private boolean conversations = true;


  public UserNeed() {
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
}
