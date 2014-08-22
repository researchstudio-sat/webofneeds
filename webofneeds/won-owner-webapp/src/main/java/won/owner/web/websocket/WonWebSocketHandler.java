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

package won.owner.web.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import won.owner.messaging.OwnerClientOut;
import won.owner.service.impl.OwnerService;
import won.protocol.message.MessageEvent;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;

import java.io.IOException;
import java.util.Set;

/**
 * User: syim
 * Date: 06.08.14
 */
public class WonWebSocketHandler
    extends TextWebSocketHandler
    implements OwnerClientOut
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private OwnerService ownerService;

  @Autowired
  private WebSocketSessionService webSocketSessionService;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException
  {
    logger.debug("OA Server - WebSocket message received: {}", message.getPayload());

    WonMessage wonMessage = WonMessageDecoder.decodeFromJsonLd(message.getPayload());

    webSocketSessionService.addMapping(
        wonMessage.getMessageEvent().getSenderURI(),
        session);

    ownerService.handleMessageEventFromClient(wonMessage);
  }

  // ToDo (FS): replace return value with something meaningful
  public void sendMessage(WonMessage wonMessage)
  {
    String wonMessageJsonLdString = WonMessageEncoder.encodeAsJsonLd(wonMessage);
    logger.debug("OA Server - sending WebSocket message: {}", wonMessageJsonLdString);

    WebSocketMessage<String> webSocketMessage = new TextMessage(wonMessageJsonLdString);

    Set<WebSocketSession> webSocketSessions =
        webSocketSessionService.getWebSocketSessions(wonMessage.getMessageEvent().getReceiverURI());

    for (WebSocketSession session : webSocketSessions)
      try {
        session.sendMessage(webSocketMessage);
      } catch (IOException e) {
        // ToDo (FS): proper handling when message could not be send (remove session from list; inform someone)
        logger.info("caught IOException:", e);
      }
  }
}
