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

package won.owner.protocol.message.impl;

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.owner.protocol.message.WonEventCallback;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.util.RdfUtils;

/**
 * Maps incoming messages from the WonMessageProcessor interface to the WonEventCallback interface.
 * Outgoing messages sent by calling the adaptee's send(msg) method are delegated to the
 */
public abstract class WonMessageHandlerAdapter implements WonMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private WonEventCallback adaptee;

  public WonMessageHandlerAdapter(final WonEventCallback adaptee) {
    this.adaptee = adaptee;
  }

  /**
   * Creates a connection object representing the connection on which the message was
   * received.
   *
   * @param wonMessage
   * @return
   */
  protected abstract Connection makeConnection(final WonMessage wonMessage);

  /**
   * Creates a Match object representing the the specified hint message.
   *
   * @param wonMessage
   * @return
   */
  protected abstract Match makeMatch(final WonMessage wonMessage);

  @Override
  public void process(final WonMessage message) throws WonMessageProcessingException {
    logger.debug("processing message {} and calling appropriate method on adaptee", message.getMessageURI() );
    WonMessageType messageType = message.getMessageType();
    switch(messageType){
      case HINT_MESSAGE:
        adaptee.onHintFromMatcher(makeMatch(message), message);
        break;
      case CONNECT:
        adaptee.onConnectFromOtherNeed(makeConnection(message), message);
        break;
      case OPEN:
        adaptee.onOpenFromOtherNeed(makeConnection(message), message);
        break;
      case CONNECTION_MESSAGE:
        adaptee.onMessageFromOtherNeed(makeConnection(message), message);
        break;
      case CLOSE:
        adaptee.onCloseFromOtherNeed(makeConnection(message), message);
        break;
      case SUCCESS_RESPONSE:
        adaptee.onSuccessResponse(message.getIsResponseToMessageURI(), message);
        break;
      case FAILURE_RESPONSE:
        adaptee.onFailureResponse(message.getIsResponseToMessageURI(), message);
        break;
      default :
        logger.info("could not find callback method for wonMessage of type {}", messageType);
        if (logger.isDebugEnabled()){
          logger.debug("message: {}", RdfUtils.writeDatasetToString(message.getCompleteDataset(), Lang.TRIG));
        }
    }
  }
}
