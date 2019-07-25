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
package won.matcher.protocol.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.jms.CamelConfiguration;
import won.protocol.jms.MatcherProtocolCommunicationService;
import won.protocol.jms.MessagingService;
import won.protocol.matcher.MatcherProtocolAtomServiceClientSide;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.util.RdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * User: gabriel Date: 12.02.13 Time: 17:26
 */
public class MatcherProtocolAtomServiceClientJMSBased implements MatcherProtocolAtomServiceClientSide {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private MessagingService messagingService;
    private MatcherProtocolCommunicationService matcherProtocolCommunicationService;
    private String startingEndpoint;

    @Override
    public void hint(URI atomURI, URI otherAtom, double score, URI originator, Model content, WonMessage wonMessage)
                    throws Exception {
        logger.info("atom-facing: HINT called for atomURI {} and otherAtom {} " + "with score {} from originator {}.",
                        new Object[] { atomURI, otherAtom, score, originator });
        CamelConfiguration camelConfiguration = matcherProtocolCommunicationService
                        .configureCamelEndpoint(wonMessage.getRecipientNodeURI(), startingEndpoint);
        String endpoint = camelConfiguration.getEndpoint();
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("atomURI", atomURI.toString());
        headerMap.put("otherAtomURI", otherAtom.toString());
        headerMap.put("score", String.valueOf(score));
        headerMap.put("originator", originator.toString());
        headerMap.put("content", RdfUtils.toString(content));
        headerMap.put("remoteBrokerEndpoint", endpoint);
        headerMap.put("methodName", "hint");
        messagingService.sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG),
                        startingEndpoint);
    }

    @Override
    public void initializeDefault() {
        // matcherProtocolCommunicationService =
    }

    public void setStartingEndpoint(String startingEndpoint) {
        this.startingEndpoint = startingEndpoint;
    }

    public MessagingService getMessagingService() {
        return messagingService;
    }

    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    public MatcherProtocolCommunicationService getMatcherProtocolCommunicationService() {
        return matcherProtocolCommunicationService;
    }

    public void setMatcherProtocolCommunicationService(
                    MatcherProtocolCommunicationService matcherProtocolCommunicationService) {
        this.matcherProtocolCommunicationService = matcherProtocolCommunicationService;
    }
}
