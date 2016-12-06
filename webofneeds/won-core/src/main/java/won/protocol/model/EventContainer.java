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

import javax.persistence.*;
import java.net.URI;
import java.util.Collection;

@Entity
@Inheritance
@DiscriminatorColumn(name="parent_type")
@Table(name="event_container")
public abstract class EventContainer
{
  @Id
  @GeneratedValue
  @Column( name = "id" )
  protected Long id;

  @Column(name = "parent_uri", nullable = false, unique = true, updatable = false)
  @Convert( converter = URIConverter.class)
  private URI parentUri;

  @OneToMany(fetch = FetchType.LAZY)
  private Collection<MessageEventPlaceholder> events;

  @Version
  @Column(name="version", columnDefinition = "integer DEFAULT 0", nullable = false)
  private long version = 0L;

  public EventContainer() {
  }

  public EventContainer(final URI parentUri) {
    this.parentUri = parentUri;
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

  public long getVersion() {
    return version;
  }

  public void setVersion(final long version) {
    this.version = version;
  }

  public URI getParentUri() {
    return parentUri;
  }

  public void setParentUri(final URI parentUri) {
    this.parentUri = parentUri;
  }
}
