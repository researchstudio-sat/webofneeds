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

package won.owner.service.impl;

import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 09.11.12
 */
public class MockMatchingService
{
  private MatcherProtocolNeedService matcherProtocolNeedService;
  private MatcherProtocolNeedService matcherProtocolNeedService2;
  private URI matcherUri;


  public void hint(final URI needURI, final URI otherNeed, final double score) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    matcherProtocolNeedService.hint(needURI, otherNeed, score, matcherUri);
  }

  public void hint2(final URI needURI, final URI otherNeed, final double score) throws NoSuchNeedException, IllegalMessageForNeedStateException
  {
    matcherProtocolNeedService2.hint(needURI, otherNeed, score, matcherUri);
  }

  public void setMatcherProtocolNeedService(final MatcherProtocolNeedService matcherProtocolNeedService)
  {
    this.matcherProtocolNeedService = matcherProtocolNeedService;
  }

  public void setMatcherProtocolNeedService2(final MatcherProtocolNeedService matcherProtocolNeedService2)
  {
    this.matcherProtocolNeedService2 = matcherProtocolNeedService2;
  }

  public void setMatcherUri(final URI matcherUri)
  {
    this.matcherUri = matcherUri;
  }
}
