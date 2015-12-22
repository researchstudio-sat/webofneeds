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

package won.node.camel.processor.general;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;

/**
 * Wraps the wonMessage, adding the direction property.
 */
public class MessageWrappingWonMessageProcessor implements WonMessageProcessor {

  WonMessageDirection direction;

  public MessageWrappingWonMessageProcessor(WonMessageDirection direction) {
    this.direction = direction;
  }

  @Override
  public WonMessage process(WonMessage message) throws WonMessageProcessingException {
   return WonMessageBuilder.wrapMessageReceivedByNode(message, direction);
  }

}
