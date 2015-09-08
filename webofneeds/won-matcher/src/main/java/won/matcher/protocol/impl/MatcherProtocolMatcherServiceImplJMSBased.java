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

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.KeyStoreService;
import won.cryptography.service.TrustStoreService;
import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.MatcherProtocolMatcherService;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.jms.MatcherProtocolCommunicationService;
import won.protocol.util.RdfUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
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
public class MatcherProtocolMatcherServiceImplJMSBased
{//implements MatcherProtocolMatcherService {

  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private MatcherNodeURISource matcherNodeURISource;

  @Autowired
  MatcherProtocolMatcherService delegate;

  private MatcherProtocolSecureCommunicationServiceImpl matcherProtocolCommunicationService;


    //TODO: [msg-refactoring] process only WonMessage, don't send additional headers

    public void needCreated(@Header("wonNodeURI") final String wonNodeURI,
                            @Header("needURI") final String needURI,
                            @Body final String content) {
        logger.debug("new need received: {} with content {}", needURI, content);

        delegate.onNewNeed(URI.create(wonNodeURI), URI.create(needURI), RdfUtils.toDataset(content));
    }
    public void needActivated(@Header("wonNodeURI") final String wonNodeURI,
                              @Header("needURI") final String needURI) {
      logger.debug("need activated message received: {}", needURI);

      delegate.onNeedActivated(URI.create(wonNodeURI), URI.create(needURI));
    }
    public void needDeactivated(@Header("wonNodeURI") final String wonNodeURI,
                                @Header("needURI") final String needURI) {
      logger.debug("need deactivated message received: {}", needURI);

      delegate.onNeedDeactivated(URI.create(wonNodeURI), URI.create(needURI));
    }

  private Set<String> configureMatcherProtocolOutTopics(URI nodeUri) throws CamelConfigurationFailedException {

    // register with remote node in order to exchange certificates if necessary. IF the same trust strategy will
    // be used when doing GET on won resource, we probably don't need this separate register step

    KeyManager km = null;
    TrustManager tm = null;
    try {
      registerMatcherAtRemoteNode(nodeUri.toString(), matcherProtocolCommunicationService.getKeyStoreService(), matcherProtocolCommunicationService.getTrustStoreService());
      // initialize key and trust managers and pass them to configuration
      String keyAlias = this.matcherProtocolCommunicationService.getKeyStoreService().getDefaultAlias();
      //TODO handle password
      km = CryptographyUtils.initializeKeyManager(matcherProtocolCommunicationService.getKeyStoreService(),
                                                             "temp", keyAlias);
      tm = CryptographyUtils.initializeTrustManager(
        matcherProtocolCommunicationService.getTrustStoreService(), nodeUri
          .toString());

    } catch (Exception e) {
      throw new CamelConfigurationFailedException("Registration at node failed", e);
    }

    Set<String> remoteTopics = matcherProtocolCommunicationService.getMatcherProtocolOutTopics (nodeUri);
    matcherProtocolCommunicationService.addRemoteTopicListeners(remoteTopics, nodeUri, km, tm);
    delegate.onMatcherRegistration(nodeUri);
    return remoteTopics;
  }

  private void registerMatcherAtRemoteNode(final String remoteNodeUri, KeyStoreService keyStoreService,
                                           TrustStoreService trustStoreService) throws
    Exception {
    // TODO handle password correctly
    RestTemplate restTemplate = CryptographyUtils.createSslTofuRestTemplate(keyStoreService, "temp",
                                                                            trustStoreService);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.TEXT_PLAIN));
    HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
    //TODO make URI configurable
    ResponseEntity<String> result = restTemplate.exchange(remoteNodeUri + "?register=node", HttpMethod
                                                            .POST,
                                                          entity,
                                                          String.class);
    logger.info("Registration status: " + result.getStatusCode());
    if (!result.getStatusCode().is2xxSuccessful()) {
      throw new IOException("Registration by remote node " + remoteNodeUri + " failed: " + result.toString());
    }
  }


  public void register(final URI wonNodeURI) {
      try {
        new Thread(){
          @Override
          public void run() {
            try {
                 configureMatcherProtocolOutTopics(wonNodeURI);
            } catch (Exception e) {
              logger.warn("Could not get topic lists from won node {}", wonNodeURI,e);
            }
          }
        }.start();
      } catch (Exception e) {
        logger.warn("getting topic lists from the node {} failed",wonNodeURI);
      }

  }
  public void register(){
      logger.debug("registering owner application on application event");
      try {
        new Thread(){
          @Override
          public void run() {
              Iterator iter = matcherNodeURISource.getNodeURIIterator();
              while (iter.hasNext()){
                URI wonNodeUri = (URI)iter.next();
                try {
                  configureMatcherProtocolOutTopics(wonNodeUri);
                } catch (Exception e) {
                  logger.warn("Could not get topic lists from default node {}", wonNodeUri,e);
                }
              }
          }
        }.start();
      } catch (Exception e) {
        logger.warn("could not register",e );
      }
  }

  public MatcherProtocolCommunicationService getMatcherProtocolCommunicationService() {
    return matcherProtocolCommunicationService;
  }

  public void setMatcherProtocolCommunicationService(final MatcherProtocolSecureCommunicationServiceImpl
                                                       matcherProtocolCommunicationService) {
    this.matcherProtocolCommunicationService = matcherProtocolCommunicationService;
  }

  public void setMatcherNodeURISource(final MatcherNodeURISource matcherNodeURISource) {
    this.matcherNodeURISource = matcherNodeURISource;
  }


}
