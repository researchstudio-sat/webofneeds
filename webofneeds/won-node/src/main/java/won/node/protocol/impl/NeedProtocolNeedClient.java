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

package won.node.protocol.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.need.NeedProtocolNeedClientSide;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
//TODO: refactor NeedProtocolNeedService into two interfaces, NeedProtocolNeedService and NeedProtocolNeedServiceClientSide, to be consistent with other protocol interfaces.
public class NeedProtocolNeedClient implements NeedProtocolNeedClientSide
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private NeedProtocolNeedClientFactory clientFactory;

  private NeedProtocolNeedClientSide delegate;

  @Override
  public ListenableFuture<URI> connect(final URI needUri, final URI otherNeedUri,
                                       final URI otherConnectionUri,
                                       final Model content, final WonMessage wonMessage) throws Exception {

    logger.debug("need to need: CONNECT called for other need {}, own need {}, own connection {}, and content {}",
        new Object[]{needUri, otherNeedUri, otherConnectionUri, content});
     return delegate.connect(needUri, otherNeedUri, otherConnectionUri, content, wonMessage);

  }

    @Override
  public void open(final Connection connection, final Model content, final WonMessage wonMessage) throws Exception {
      logger.debug("need to need: OPEN called for connection {}", connection);
      delegate.open(connection, content, wonMessage);
  }

  @Override
  public void close(final Connection connection, final Model content, final WonMessage wonMessage) throws Exception {
    logger.debug("need to need: CLOSE called for connection {}", connection);
    delegate.close(connection, content, wonMessage);

  }

  @Override
  public void sendMessage(final Connection connection, final Model message, final WonMessage wonMessage)
          throws Exception {
    logger.debug("need to need: SEND_TEXT_MESSAGE called for connection {} with message {}", connection, message);
    delegate.sendMessage(connection, message, wonMessage);

  }

  public void setClientFactory(final NeedProtocolNeedClientFactory clientFactory)
  {
    this.clientFactory = clientFactory;
  }


    public void setDelegate(NeedProtocolNeedClientSide delegate) {
        this.delegate = delegate;
    }
}