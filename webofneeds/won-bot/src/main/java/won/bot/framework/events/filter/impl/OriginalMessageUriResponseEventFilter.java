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

package won.bot.framework.events.filter.impl;

import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.ResponseEvent;
import won.bot.framework.events.filter.EventFilter;
import won.protocol.message.WonMessage;

import java.net.URI;

/**
 * Accepts only ResponseEvents with the specified originalMessageURI.
 */
public class OriginalMessageUriResponseEventFilter implements EventFilter
{
  private URI originalMessageURI;

  public OriginalMessageUriResponseEventFilter(final URI originalMessageURI) {
    this.originalMessageURI = originalMessageURI;
  }

  public static OriginalMessageUriResponseEventFilter forWonMessage(WonMessage wonMessage){
    return new OriginalMessageUriResponseEventFilter(wonMessage.getMessageURI());
  }

  @Override
  public boolean accept(final Event event) {
    if (event instanceof ResponseEvent){
      URI messageURI = ((ResponseEvent)event).getOriginalMessageURI();
      if (this.originalMessageURI.equals(messageURI)) return true;
    }
    return false;
  }
}
