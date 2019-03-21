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

package won.bot.framework.eventbot.event;

import java.net.URI;

/**
 * Event indicating that the attempt to create a need on the specified won node
 * failed.
 */
public class NeedCreationFailedEvent extends BaseEvent {
  // the URI the need had before a new need uri was created
  private URI needUriBeforeCreation;
  private URI wonNodeURI;

  public NeedCreationFailedEvent(final URI wonNodeUri) {
    this(wonNodeUri, null);
  }

  public NeedCreationFailedEvent(URI needUriBeforeCreation, URI wonNodeURI) {
    this.needUriBeforeCreation = needUriBeforeCreation;
    this.wonNodeURI = wonNodeURI;
  }

  public URI getWonNodeURI() {
    return wonNodeURI;
  }

  public URI getNeedUriBeforeCreation() {
    return needUriBeforeCreation;
  }
}
