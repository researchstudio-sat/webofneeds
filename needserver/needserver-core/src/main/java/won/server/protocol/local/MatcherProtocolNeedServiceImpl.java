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

package won.server.protocol.local;

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedService;
import won.protocol.model.Connection;
import won.protocol.model.Need;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class MatcherProtocolNeedServiceImpl implements MatcherProtocolNeedService
{
  private MatcherProtocolNeedService matcherProtocolNeedService;

  @Override
  public void hint(final URI needURI, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    matcherProtocolNeedService.hint(needURI, otherNeed, score, originator);
  }

  @Override
  public Collection<URI> listNeedURIs()
  {
    return matcherProtocolNeedService.listNeedURIs();
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return matcherProtocolNeedService.listConnectionURIs(needURI);
  }

  @Override
  public Need readNeed(final URI needURI) throws NoSuchNeedException
  {
    return matcherProtocolNeedService.readNeed(needURI);
  }

  @Override
  public Graph readNeedContent(final URI needURI) throws NoSuchNeedException
  {
    return matcherProtocolNeedService.readNeedContent(needURI);
  }

  @Override
  public Connection readConnection(final URI connectionURI) throws NoSuchNeedException
  {
    return matcherProtocolNeedService.readConnection(connectionURI);
  }

  @Override
  public Graph readConnectionContent(final URI connectionURI) throws NoSuchNeedException
  {
    return matcherProtocolNeedService.readConnectionContent(connectionURI);
  }

  public void setMatcherProtocolNeedService(final MatcherProtocolNeedService matcherProtocolNeedService)
  {
    this.matcherProtocolNeedService = matcherProtocolNeedService;
  }
}
