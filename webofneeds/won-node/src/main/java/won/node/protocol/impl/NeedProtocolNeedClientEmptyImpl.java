/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.node.protocol.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.*;
import won.protocol.need.NeedProtocolNeedService;

import java.net.URI;
import java.text.MessageFormat;

/**
 * TODO: empty need client implementation to be replaced by WS client
 */
public class NeedProtocolNeedClientEmptyImpl implements NeedProtocolNeedService
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public URI connectionRequested(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info("need-facing: CONNECTION_REQUESTED called for own need {}, other need {}, other connection {} and message {}",
        new Object[]{needURI, otherNeedURI, otherConnectionURI, message});
    return null;
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("need-facing: CLOSE called for connection {}", connectionURI);
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
  }
}
