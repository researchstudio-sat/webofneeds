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

package won.bot.framework.eventbot.event.impl.command.connect;

import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
import won.bot.framework.eventbot.event.RemoteNeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Instructs the bot to connect to the specified remoteNeed on behalf of the
 * need.
 */
public class ConnectCommandEvent extends BaseNeedSpecificEvent implements MessageCommandEvent, RemoteNeedSpecificEvent {
  private URI remoteNeedURI;
  private Optional<URI> localFacet = Optional.empty();
  private Optional<URI> remoteFacet = Optional.empty();
  private String welcomeMessage;

  public ConnectCommandEvent(URI needURI, URI remoteNeedURI, URI localFacet, URI remoteFacet, String welcomeMessage) {
    super(needURI);
    Objects.requireNonNull(localFacet);
    Objects.requireNonNull(remoteFacet);
    this.remoteNeedURI = remoteNeedURI;
    this.localFacet = Optional.of(localFacet);
    this.remoteFacet = Optional.of(remoteFacet);
    this.welcomeMessage = welcomeMessage;
  }

  public ConnectCommandEvent(URI needURI, URI remoteNeedURI, String welcomeMessage) {
    super(needURI);
    this.remoteNeedURI = remoteNeedURI;
    this.welcomeMessage = welcomeMessage;
  }

  public ConnectCommandEvent(URI needURI, URI remoteNeedURI) {
    this(needURI, remoteNeedURI, "Hello!");
  }

  @Override
  public WonMessageType getWonMessageType() {
    return WonMessageType.CONNECT;
  }

  public URI getRemoteNeedURI() {
    return remoteNeedURI;
  }

  public Optional<URI> getLocalFacet() {
    return localFacet;
  }

  public Optional<URI> getRemoteFacet() {
    return remoteFacet;
  }

  public String getWelcomeMessage() {
    return welcomeMessage;
  }
}