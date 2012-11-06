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

package won.server.protocol.impl;

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedService;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.service.MatcherFacingNeedCommunicationService;
import won.protocol.service.NeedInformationService;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class MatcherProtocolNeedServiceImpl implements MatcherProtocolNeedService
{
  private MatcherFacingNeedCommunicationService matcherFacingNeedCommunicationService;
  private NeedInformationService needInformationService;

  @Override
  public void hint(final URI needURI, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    matcherFacingNeedCommunicationService.hint(needURI,otherNeed,score,originator);
  }

  @Override
  public Collection<URI> listNeedURIs()
  {
    return needInformationService.listNeedURIs();
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return needInformationService.listConnectionURIs(needURI);
  }

  @Override
  public Need readNeed(final URI needURI) throws NoSuchNeedException
  {
    return needInformationService.readNeed(needURI);
  }

  @Override
  public Graph readNeedContent(final URI needURI) throws NoSuchNeedException
  {
    return needInformationService.readNeedContent(needURI);
  }

  @Override
  public Connection readConnection(final URI connectionURI) throws NoSuchNeedException
  {
    return needInformationService.readConnection(connectionURI);
  }

  @Override
  public Graph readConnectionContent(final URI connectionURI) throws NoSuchNeedException
  {
    return needInformationService.readConnectionContent(connectionURI);
  }

  public void setMatcherProtocolNeedService(final MatcherProtocolNeedService matcherProtocolNeedService)
  {
    this.needInformationService = matcherProtocolNeedService;
  }

  public void setMatcherFacingNeedCommunicationService(final MatcherFacingNeedCommunicationService matcherFacingNeedCommunicationService)
  {
    this.matcherFacingNeedCommunicationService = matcherFacingNeedCommunicationService;
  }

  public void setNeedInformationService(final NeedInformationService needInformationService)
  {
    this.needInformationService = needInformationService;
  }

}
