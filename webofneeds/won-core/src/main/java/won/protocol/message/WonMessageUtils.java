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

package won.protocol.message;

import java.net.URI;

/**
 * Utilities for working with wonMessage objects.
 */
public class WonMessageUtils
{
  public static URI getParentEntityUri(final WonMessage message) {
    URI parentURI = null;
    WonMessageDirection direction = message.getEnvelopeType();
    if (direction == WonMessageDirection.FROM_EXTERNAL ){
      parentURI = getParentUriFromReceiverProperties(message);
    } else if (direction == WonMessageDirection.FROM_OWNER || direction == WonMessageDirection.FROM_SYSTEM){
      parentURI = getParentUriFromSenderProperties(message);
    }
    return parentURI;
  }

  private static URI getParentUriFromSenderProperties(WonMessage message) {
    URI parentURI;
    parentURI = message.getSenderURI();
    if (parentURI == null) {
      parentURI = message.getSenderNeedURI();
    }
    if (parentURI == null) {
      parentURI = message.getSenderNodeURI();
    }
    return parentURI;
  }

  private static URI getParentUriFromReceiverProperties(WonMessage message) {
    URI parentURI;
    parentURI = message.getReceiverURI();
    if (parentURI == null) {
      parentURI = message.getReceiverNeedURI();
    }
    if (parentURI == null) {
      parentURI = message.getReceiverNodeURI();
    }
    return parentURI;
  }
}
