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

import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.MatcherProtocolMatcherService;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.jms.MatcherProtocolCommunicationService;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.Iterator;
import java.util.Set;


/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:12
 */
//TODO copied from OwnerProtocolOwnerService... refactoring needed
    //TODO: refactor service interfaces.
public class MatcherProtocolMatcherServiceImplJMSBased implements ApplicationListener<ContextRefreshedEvent>
{//implements MatcherProtocolMatcherService {

  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private WonNodeRepository wonNodeRepository;

  @Autowired
  private MatcherNodeURISource matcherNodeURISource;

  @Autowired
  MatcherProtocolMatcherService delegate;

  private MatcherProtocolCommunicationService matcherProtocolCommunicationService;

  private URI defaultNodeURI;

  private boolean onApplicationRun = false;


    public void needCreated(@Header("needURI") final String needURI, @Header("content") final String content) {
        logger.debug("new need received: {} with content {}", needURI, content);

        delegate.onNewNeed(URI.create(needURI), RdfUtils.toModel(content));
    }
    public void needActivated(@Header("needURI") final String needURI) {
      logger.debug("need activated message received: {}", needURI);

      delegate.onNeedActivated(URI.create(needURI));
    }
    public void needDeactivated(@Header("needURI") final String needURI) {
      logger.debug("need deactivated message received: {}", needURI);

      delegate.onNeedDeactivated(URI.create(needURI));
    }


  private void configureMatcherProtocolOutTopics(URI wonNodeUri) throws CamelConfigurationFailedException {
    Set<String> remoteTopics = matcherProtocolCommunicationService.getMatcherProtocolOutTopics (wonNodeUri);
    matcherProtocolCommunicationService.addRemoteTopicListeners(remoteTopics,wonNodeUri);
  }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (!onApplicationRun){
            logger.debug("registering owner application on application event");
            try {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                          Iterator iter = matcherNodeURISource.getNodeURIIterator();
                          while (iter.hasNext()){
                            configureMatcherProtocolOutTopics((URI)iter.next());
                          }

                        } catch (Exception e) {
                            logger.warn("Could not get topic lists from default node {}", defaultNodeURI,e);
                    }
                    }
                }.start();
            } catch (Exception e) {
                logger.warn("getting topic lists from the node {} failed",defaultNodeURI);
            }
            onApplicationRun = true;
        }


    }

  public MatcherProtocolCommunicationService getMatcherProtocolCommunicationService() {
    return matcherProtocolCommunicationService;
  }

  public void setMatcherProtocolCommunicationService(final MatcherProtocolCommunicationService matcherProtocolCommunicationService) {
    this.matcherProtocolCommunicationService = matcherProtocolCommunicationService;
  }

  public void setDefaultNodeURI(final URI defaultNodeURI) {
    this.defaultNodeURI = defaultNodeURI;
  }

  public void setMatcherNodeURISource(final MatcherNodeURISource matcherNodeURISource) {
    this.matcherNodeURISource = matcherNodeURISource;
  }


}
