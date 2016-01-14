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

package won.bot.framework.events.event.impl;

import won.bot.framework.events.event.BaseEvent;
import won.bot.framework.events.event.ResponseEvent;
import won.protocol.message.WonMessage;

import java.net.URI;

/**
 * Event published whenever a WonMessage is received that indicates the failure of a previous message.
 */
public class SuccessResponseEvent extends BaseEvent implements ResponseEvent
{
  private URI originalMessageURI;
  private WonMessage successMessage;

  public SuccessResponseEvent(URI originalMessageURI, WonMessage successMessage) {
    assert originalMessageURI != null : "originalMessageURI must not be null!";
    assert successMessage != null : "successMessage must not be null!";
    this.originalMessageURI = originalMessageURI;
    this.successMessage = successMessage;
  }

  public URI getOriginalMessageURI() {
    return originalMessageURI;
  }

  public WonMessage getSuccessMessage() {
    return successMessage;
  }

  @Override
  public URI getConnectionURI() {
    return successMessage.getReceiverURI();
  }

  @Override
  public URI getRemoteNeedURI() {
    return successMessage.getSenderNeedURI();
  }

  @Override
  public URI getNeedURI() {
    return successMessage.getReceiverNeedURI();
  }
}
