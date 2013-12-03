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

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.jms.MessagingService;
import won.protocol.owner.OwnerProtocolOwnerServiceClientSide;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;

import java.net.URI;
import java.text.MessageFormat;

public class OwnerProtocolOwnerClient implements OwnerProtocolOwnerServiceClientSide
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  private OwnerProtocolOwnerServiceClientSide delegate;

  @Autowired
  private OwnerProtocolOwnerClientFactory clientFactory;

  private MessagingService messagingService;

  @Autowired
  private NeedRepository needRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
  public void hint(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    logger.info(MessageFormat.format("owner-facing: HINT_RECEIVED called for own need {0}, other need {1}, with score {2} from originator {3} and content {4}", ownNeedURI, otherNeedURI, score, originatorURI, content));
    delegate.hint(ownNeedURI,otherNeedURI,score,originatorURI,content);
  }


    @Override
    public void connect(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final Model content) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
    {
      logger.info(MessageFormat.format("ownerww-facing: CONNECTION_REQUESTED called for own need {0}, other need {1}, own connection {2} and message ''{3}''", ownNeedURI, otherNeedURI, ownConnectionURI, content));
      delegate.connect(ownNeedURI,otherNeedURI,ownConnectionURI,content);

    }
    @Override
  public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("owner-facing: Open called for connection {0}", connectionURI));
    delegate.open(connectionURI,content);

  }


  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("owner-facing: CLOSE called for connection {0}", connectionURI));
    delegate.close(connectionURI,content);

  }

  @Override
  public void textMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("owner-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}", connectionURI, message));
    delegate.textMessage(connectionURI,message);

  }

  public void setClientFactory(final OwnerProtocolOwnerClientFactory clientFactory)
  {
    this.clientFactory = clientFactory;
  }

    public void setNeedRepository(NeedRepository needRepository) {
        this.needRepository = needRepository;
    }

    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    public void setDelegate(OwnerProtocolOwnerServiceClientSide delegate) {
        this.delegate = delegate;
    }
}