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

package won.matcher.protocol.impl;

import java.net.URI;
import java.util.Iterator;
import java.util.Set;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.MatcherProtocolMatcherService;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.jms.MatcherProtocolCommunicationService;
import won.protocol.util.RdfUtils;

/**
 * Created with IntelliJ IDEA. User: Gabriel Date: 03.12.12 Time: 14:12
 */
// TODO copied from OwnerProtocolOwnerService... refactoring needed
// TODO: refactor service interfaces.
public class MatcherProtocolMatcherServiceImplJMSBased {// implements MatcherProtocolMatcherService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MatcherNodeURISource matcherNodeURISource;

    @Autowired
    MatcherProtocolMatcherService delegate;

    private MatcherProtocolCommunicationServiceImpl matcherProtocolCommunicationService;

    // TODO: [msg-refactoring] process only WonMessage, don't send additional headers

    public void needCreated(@Header("wonNodeURI") final String wonNodeURI, @Header("needURI") final String needURI,
            @Body final String content) {
        logger.debug("new need received: {} with content {}", needURI, content);

        delegate.onNewNeed(URI.create(wonNodeURI), URI.create(needURI), RdfUtils.toDataset(content));
    }

    public void needActivated(@Header("wonNodeURI") final String wonNodeURI, @Header("needURI") final String needURI) {
        logger.debug("need activated message received: {}", needURI);

        delegate.onNeedActivated(URI.create(wonNodeURI), URI.create(needURI));
    }

    public void needDeactivated(@Header("wonNodeURI") final String wonNodeURI,
            @Header("needURI") final String needURI) {
        logger.debug("need deactivated message received: {}", needURI);

        delegate.onNeedDeactivated(URI.create(wonNodeURI), URI.create(needURI));
    }

    private Set<String> configureMatcherProtocolOutTopics(URI nodeUri) throws CamelConfigurationFailedException {

        Set<String> remoteTopics = matcherProtocolCommunicationService.getMatcherProtocolOutTopics(nodeUri);
        matcherProtocolCommunicationService.addRemoteTopicListeners(remoteTopics, nodeUri);
        delegate.onMatcherRegistration(nodeUri);
        return remoteTopics;
    }

    public void register(final URI wonNodeURI) throws CamelConfigurationFailedException {
        configureMatcherProtocolOutTopics(wonNodeURI);
    }

    public void register() {
        logger.debug("registering owner application on application event");
        try {
            new Thread() {
                @Override
                public void run() {
                    Iterator iter = matcherNodeURISource.getNodeURIIterator();
                    while (iter.hasNext()) {
                        URI wonNodeUri = (URI) iter.next();
                        try {
                            configureMatcherProtocolOutTopics(wonNodeUri);
                        } catch (Exception e) {
                            logger.warn("Could not get topic lists from default node {}", wonNodeUri, e);
                        }
                    }
                }
            }.start();
        } catch (Exception e) {
            logger.warn("could not register", e);
        }
    }

    public MatcherProtocolCommunicationService getMatcherProtocolCommunicationService() {
        return matcherProtocolCommunicationService;
    }

    public void setMatcherProtocolCommunicationService(
            final MatcherProtocolCommunicationServiceImpl matcherProtocolCommunicationService) {
        this.matcherProtocolCommunicationService = matcherProtocolCommunicationService;
    }

    public void setMatcherNodeURISource(final MatcherNodeURISource matcherNodeURISource) {
        this.matcherNodeURISource = matcherNodeURISource;
    }

}
