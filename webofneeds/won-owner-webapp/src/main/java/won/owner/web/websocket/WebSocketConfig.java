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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * User: LEIH-NB Date: 06.08.14
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer// extends AbstractWebSocketMessageBrokerConfigurer
{
  private WonWebSocketHandler wonWebSocketHandler;

  @Override
  public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
    registry.addHandler(this.wonWebSocketHandler, "/msg").addInterceptors(new WonHandshakeInterceptor()).withSockJS();
  }

  @Autowired
  public void setWonWebSocketHandler(WonWebSocketHandler wonWebSocketHandler) {
    this.wonWebSocketHandler = wonWebSocketHandler;
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    // here, we set the buffer size of each websocket. This means that we will
    // allocate the
    // specified amount of memory for each browser session. Of course we would like
    // to pre-allocate as little as
    // possible, but we haven't figured out a way to do that yet. We use websockets
    // via spring's sockjs
    // implementation, and they do not allow partial messages, and there doesn't
    // seem to be another way.
    int bufferSize = 4 * 1024 * 1024; // 4MB, so we can have attachments (images)
    container.setMaxTextMessageBufferSize(bufferSize);
    // don't need a binary buffer - or so we think: beware, if this is too small,
    // our application fails - silently. How
    // great is that?
    container.setMaxBinaryMessageBufferSize(bufferSize);

    return container;
  }

}
