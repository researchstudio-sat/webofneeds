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

package won.bot.framework.eventbot.event.impl.command.create;

import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;

import java.net.URI;

/**
 * Indicates that need creation failed.
 */
public class CreateNeedCommandFailureEvent extends BaseNeedSpecificEvent
    implements MessageCommandFailureEvent, CreateNeedCommandResultEvent {
  private final URI needUriBeforeCreation;
  private CreateNeedCommandEvent createNeedCommandEvent;
  private String message;

  public CreateNeedCommandFailureEvent(URI needURI, URI needUriBeforeCreation,
      CreateNeedCommandEvent createNeedCommandEvent, String message) {
    super(needURI);
    this.needUriBeforeCreation = needUriBeforeCreation;
    this.createNeedCommandEvent = createNeedCommandEvent;
    this.message = message;
  }

  public CreateNeedCommandFailureEvent(URI needURI, URI needUriBeforeCreation,
      CreateNeedCommandEvent createNeedCommandEvent) {
    super(needURI);
    this.needUriBeforeCreation = needUriBeforeCreation;
    this.createNeedCommandEvent = createNeedCommandEvent;
  }

  @Override public MessageCommandEvent getOriginalCommandEvent() {
    return createNeedCommandEvent;
  }

  public URI getNeedUriBeforeCreation() {
    return needUriBeforeCreation;
  }

  @Override public String getMessage() {
    return message;
  }

  @Override public boolean isSuccess() {
    return false;
  }
}
