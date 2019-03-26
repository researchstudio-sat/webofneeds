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

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;

public class MatcherProtocolMatcherClientImpl implements MatcherProtocolMatcherServiceClientSide {
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private MessagingService messagingService;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private ConnectionRepository connectionRepository;

  private MatcherProtocolMatcherServiceClientSide delegate;

  @Override
  public void matcherRegistered(final URI wonNodeURI, final WonMessage wonMessage) {
    logger.debug("calling matcherRegistered");
    delegate.matcherRegistered(wonNodeURI, wonMessage);
  }

  @Override
  public void needCreated(final URI needURI, final Model content, final WonMessage wonMessage) {
    logger.debug("calling needCreated for needURI {}", needURI);
    delegate.needCreated(needURI, content, wonMessage);

  }

  @Override
  public void needActivated(final URI needURI, final WonMessage wonMessage) {
    logger.debug("calling needActivated for needURI {}", needURI);
    delegate.needActivated(needURI, wonMessage);
  }

  @Override
  public void needDeactivated(final URI needURI, final WonMessage wonMessage) {
    logger.debug("calling needDeactivated for needURI {}", needURI);
    delegate.needDeactivated(needURI, wonMessage);
  }

  @Override
  public void needDeleted(final URI needURI, final WonMessage wonMessage) {
    logger.debug("calling needDeleted for needURI {}", needURI);
    delegate.needDeleted(needURI, wonMessage);
  }

  public void setNeedRepository(NeedRepository needRepository) {
    this.needRepository = needRepository;
  }

  public void setMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }
}