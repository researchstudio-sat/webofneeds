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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * User: LEIH-NB
 * Date: 06.08.14
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig   implements WebSocketConfigurer//extends AbstractWebSocketMessageBrokerConfigurer
{    /*
  @Bean
  public SimpleUrlHandlerMapping handlerMapping(TaskScheduler ts){
    Map<String, Object> urlMap = new HashMap<String, Object>();
    urlMap.put("/msg/**", new SockJsHttpRequestHandler(new DefaultSockJsService(ts),
                                                                    new WonWebSocketHandler()));

    SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
    hm.setOrder(0);
    hm.setUrlMap(urlMap);
    return hm;
  }           */

  @Override
  public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
    registry.addHandler(new WonWebSocketHandler(),"/msg").withSockJS();
  }
  @Bean
  public WebSocketHandler wonWebSocketHandler(){
    return new WonWebSocketHandler();
  }



}

