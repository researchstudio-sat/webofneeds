/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.node.protocol.impl;

import java.net.URI;

import javax.jws.WebMethod;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import won.protocol.matcher.MatcherProtocolAtomService;
import won.protocol.message.WonMessage;
import won.protocol.service.MatcherFacingAtomCommunicationService;

/**
 * User: fkleedorfer Date: 02.11.12
 */
@Service
public class MatcherProtocolAtomServiceImpl implements MatcherProtocolAtomService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private MatcherFacingAtomCommunicationService matcherFacingAtomCommunicationService;

    @Override
    public void hint(final URI atomURI, final URI otherAtom, final double score, final URI originator, Model content,
                    WonMessage wonMessage) throws Exception {
        logger.debug("atom from matcher: HINT received for atom {} referring to atom {} with score {} from originator {} and content {}",
                        new Object[] { atomURI, otherAtom, score, originator, content });
        matcherFacingAtomCommunicationService.hint(atomURI, otherAtom, score, originator, content, wonMessage);
    }

    @WebMethod(exclude = true)
    public void setMatcherFacingAtomCommunicationService(
                    final MatcherFacingAtomCommunicationService matcherFacingAtomCommunicationService) {
        this.matcherFacingAtomCommunicationService = matcherFacingAtomCommunicationService;
    }
}
