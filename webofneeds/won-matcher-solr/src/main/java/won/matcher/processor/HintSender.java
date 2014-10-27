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

package won.matcher.processor;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.FacetType;
import won.protocol.service.WonNodeInformationService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 06.09.13
 */
public class HintSender implements MatchProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private MatcherProtocolNeedServiceClientSide client;
  private WonNodeInformationService wonNodeInformationService;

  public HintSender(MatcherProtocolNeedServiceClientSide client, WonNodeInformationService wonNodeInformationService)
  {
    assert client != null : "client must not be null";
    assert wonNodeInformationService != null : "wonNodeInformationService must not be null";
    this.client = client;
    this.wonNodeInformationService = wonNodeInformationService;
  }

  @Override
  public void process(final URI from, final URI to, final double score, final URI originator, final Model explanation)
  {
    try {
      logger.debug("sending hint from {} to {}", from, to);
        // ToDo (FS): provide fitting messageEvent
      client.hint(from, to, score, originator, explanation,
                  createWonMessage(from, to, score, originator));
    } catch (NoSuchNeedException e) {
      logger.debug("hint failed: no need found with URI {}", e.getUnknownNeedURI());
    } catch (IllegalMessageForNeedStateException e) {
      logger.debug("hint failed: illegal state '{}' for hint to need {}", e.getNeedState(), e.getNeedURI());
    } catch (Exception e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  private WonMessage createWonMessage(URI needURI, URI otherNeedURI, double score, URI originator)
    throws WonMessageBuilderException {

    URI wonNode = wonNodeInformationService.getDefaultWonNodeURI();

    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
      .setMessagePropertiesForHint(
        wonNodeInformationService.generateEventURI(
          wonNode),
        needURI,
        FacetType.OwnerFacet.getURI(),
        wonNode,
        otherNeedURI,
        FacetType.OwnerFacet.getURI(),
        originator,
        score)
      .build();
  }
}
