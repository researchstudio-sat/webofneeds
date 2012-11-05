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

package won.matcher.protocol.local;

import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedService;

import java.net.URI;
import java.util.Collection;

/**
 * Implementation for testing purposes; communicates only with partners within the same VM.
 */
public class MatcherProtocolNeedClient implements MatcherProtocolNeedService
{

  private MatcherProtocolNeedService matcherProtocolNeedService;

  @Override
  public void hint(final URI needURI, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    this.matcherProtocolNeedService.hint(needURI, otherNeed, score, originator);
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

  public void setMatcherProtocolNeedService(final MatcherProtocolNeedService matcherProtocolNeedService)
  {
    this.matcherProtocolNeedService = matcherProtocolNeedService;
  }
}
