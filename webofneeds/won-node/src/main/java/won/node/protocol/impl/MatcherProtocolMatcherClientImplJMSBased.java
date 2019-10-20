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
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.node.service.nodeconfig.URIService;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;

public class MatcherProtocolMatcherClientImplJMSBased implements MatcherProtocolMatcherServiceClientSide {
    final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private MessagingService messagingService;
    @Autowired
    private URIService uriService;

    @Override
    public void matcherRegistered(final URI wonNodeURI, final WonMessage wonMessage) {
        Map headerMap = new HashMap<String, String>();
        headerMap.put("wonNodeURI", wonNodeURI.toString());
        headerMap.put("protocol", "MatcherProtocol");
        headerMap.put("methodName", "matcherRegistered");
        messagingService.sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG),
                        "seda:MatcherProtocolOut");
    }

    @Override
    public void atomCreated(final URI atomURI, final Model content, final WonMessage wonMessage) {
        Map headerMap = new HashMap<String, String>();
        headerMap.put("atomURI", atomURI.toString());
        headerMap.put("methodName", "atomCreated");
        headerMap.put("wonNodeURI", uriService.getGeneralURIPrefix() + "/resource");
        messagingService.sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG),
                        "seda:MatcherProtocolOut");
    }

    @Override
    public void atomModified(final URI atomURI, final WonMessage wonMessage) {
        Map headerMap = new HashMap<String, String>();
        headerMap.put("atomURI", atomURI.toString());
        headerMap.put("methodName", "atomModified");
        headerMap.put("wonNodeURI", uriService.getGeneralURIPrefix() + "/resource");
        messagingService.sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG),
                        "seda:MatcherProtocolOut");
    }

    @Override
    public void atomActivated(final URI atomURI, final WonMessage wonMessage) {
        Map headerMap = new HashMap<String, String>();
        headerMap.put("atomURI", atomURI.toString());
        headerMap.put("methodName", "atomActivated");
        headerMap.put("wonNodeURI", uriService.getGeneralURIPrefix() + "/resource");
        messagingService.sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG),
                        "seda:MatcherProtocolOut");
    }

    @Override
    public void atomDeactivated(final URI atomURI, final WonMessage wonMessage) {
        Map headerMap = new HashMap<String, String>();
        headerMap.put("atomURI", atomURI.toString());
        headerMap.put("methodName", "atomDeactivated");
        headerMap.put("wonNodeURI", uriService.getGeneralURIPrefix() + "/resource");
        messagingService.sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG),
                        "seda:MatcherProtocolOut");
    }

    @Override
    public void atomDeleted(final URI atomURI, final WonMessage wonMessage) {
        Map headerMap = new HashMap<String, String>();
        headerMap.put("atomURI", atomURI.toString());
        headerMap.put("methodName", "atomDeleted");
        headerMap.put("wonNodeURI", uriService.getGeneralURIPrefix() + "/resource");
        messagingService.sendInOnlyMessage(null, headerMap, WonMessageEncoder.encode(wonMessage, Lang.TRIG),
                        "seda:MatcherProtocolOut");
    }
}