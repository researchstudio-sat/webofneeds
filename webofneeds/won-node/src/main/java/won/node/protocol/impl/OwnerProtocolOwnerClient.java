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
import won.protocol.exception.*;
import won.protocol.owner.OwnerProtocolOwnerServiceClientSide;

import java.net.URI;

public class OwnerProtocolOwnerClient implements OwnerProtocolOwnerServiceClientSide
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  private OwnerProtocolOwnerServiceClientSide delegate;

    @Override
  public void hint(final URI ownNeedUri, final URI otherNeedUri, final double score, final URI originatorUri, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    logger.debug("need to owner: HINT for own need {}, other need {}, score {} from originator {}, content {}", new Object[]{ownNeedUri, otherNeedUri, score, originatorUri, content});
    delegate.hint(ownNeedUri, otherNeedUri,score, originatorUri,content);
  }


  @Override
  public void connect(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final Model content) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
  {
    logger.debug("need to owner: CONNECT for own need {}, other need {}, own connection {} and content {}'", new Object[]{ownNeedURI, otherNeedURI, ownConnectionURI, content});
    delegate.connect(ownNeedURI,otherNeedURI,ownConnectionURI,content);
  }
  @Override

  public void open(final URI connectionURI, final Model content)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException, IllegalMessageForNeedStateException {
    logger.debug("need to owner: OPEN for connection {}", connectionURI);
    delegate.open(connectionURI,content);
  }


  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    logger.debug("need to owner: CLOSE for connection {}", connectionURI);
    delegate.close(connectionURI,content);
  }

  @Override
  public void textMessage(final URI connectionURI, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    logger.debug("need to owner: MESSAGE for connection {} with message {}", connectionURI, message);
    delegate.textMessage(connectionURI, message);
  }

    public void setDelegate(OwnerProtocolOwnerServiceClientSide delegate) {
        this.delegate = delegate;
    }
}