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

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.stereotype.Service;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedService;
import won.protocol.service.MatcherFacingNeedCommunicationService;

import javax.jws.WebMethod;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@Service
public class MatcherProtocolNeedServiceImpl implements MatcherProtocolNeedService
{
  private MatcherFacingNeedCommunicationService matcherFacingNeedCommunicationService;

  @Override
  public void hint(final URI needURI, final URI otherNeed, final double score, final URI originator, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException {
    matcherFacingNeedCommunicationService.hint(needURI, otherNeed, score, originator, content);
  }

  @WebMethod(exclude = true)
  public void setMatcherFacingNeedCommunicationService(final MatcherFacingNeedCommunicationService matcherFacingNeedCommunicationService)
  {
    this.matcherFacingNeedCommunicationService = matcherFacingNeedCommunicationService;
  }

}
