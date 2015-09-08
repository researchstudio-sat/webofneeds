package won.matcher.protocol.impl;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.KeyStoreService;
import won.cryptography.service.TrustStoreService;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.*;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;

/**
 * User: ypanchenko
 * Date: 02.09.2015
 */
public class MatcherProtocolSecureCommunicationServiceImpl implements MatcherProtocolCommunicationService {


  private KeyStoreService keyStoreService;
  private TrustStoreService trustStoreService;

  private MatcherProtocolCamelConfigurator matcherProtocolCamelConfigurator;

  private MatcherActiveMQService activeMQService;


  private String componentName;

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public synchronized CamelConfiguration configureCamelEndpoint(URI nodeUri, String startingEndpoint) throws Exception {
    String matcherProtocolQueueName;
    CamelConfiguration camelConfiguration = new CamelConfiguration();

    URI needBrokerUri =activeMQService.getBrokerEndpoint(nodeUri);


    if (matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(needBrokerUri)!=null){
      if (matcherProtocolCamelConfigurator.getEndpoint(needBrokerUri)!=null)
      {
        camelConfiguration.setEndpoint(matcherProtocolCamelConfigurator.getEndpoint(needBrokerUri));
      } else {
        matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint,needBrokerUri);
        matcherProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(nodeUri);

        // register with remote node in order to exchange certificates if necessary. IF the same trust strategy will
        // be used when doing GET on won resource, we probably don't need this separate register step
        registerMatcherAtRemoteNode(nodeUri.toString());
        // initialize key and trust managers and pass them to configuration
        String keyAlias = keyStoreService.getDefaultAlias();
        //TODO handle password
        KeyManager km = CryptographyUtils.initializeKeyManager(keyStoreService, "temp", keyAlias);
        TrustManager tm = CryptographyUtils.initializeTrustManager(trustStoreService, nodeUri.toString());
        String endpoint = matcherProtocolCamelConfigurator.configureCamelEndpointForNeedUri(nodeUri, needBrokerUri,
                                                                                            matcherProtocolQueueName,
                                                                                         km, tm);

        camelConfiguration.setEndpoint(endpoint);
      }
      camelConfiguration.setBrokerComponentName(matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(needBrokerUri));

    } else{

      URI resourceUri = nodeUri;
      URI brokerUri = needBrokerUri;

      matcherProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
      camelConfiguration.setEndpoint(matcherProtocolCamelConfigurator.configureCamelEndpointForNeedUri(resourceUri,
                                                                                                       brokerUri,
                                                                                                       matcherProtocolQueueName));
      matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint,brokerUri);
      camelConfiguration.setBrokerComponentName(matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
      ActiveMQComponent activeMQComponent = (ActiveMQComponent)matcherProtocolCamelConfigurator.getCamelContext().getComponent(matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
      logger.info("ActiveMQ Service Status : {}",activeMQComponent.getStatus().toString());
      activeMQComponent.start();
    }
    return camelConfiguration;
  }

  private void registerMatcherAtRemoteNode(final String remoteNodeUri) throws Exception {
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
    logger.info("Registration status: " +  result.getStatusCode());
    if (!result.getStatusCode().is2xxSuccessful()) {
      throw new IOException("Registration by remote node " + remoteNodeUri + " failed: " + result.toString());
    }
  }

  @Override
  public synchronized Set<String> getMatcherProtocolOutTopics(URI wonNodeURI) {
    Set<String> matcherProtocolTopics = ((MatcherActiveMQService)activeMQService)
      .getMatcherProtocolTopicNamesWithResource(wonNodeURI);
    return matcherProtocolTopics;
  }

  @Override
  public synchronized void addRemoteTopicListeners(final Set<String> endpoints, final URI wonNodeUri)
    throws CamelConfigurationFailedException {
    URI remoteEndpoint = activeMQService.getBrokerEndpoint(wonNodeUri);
    String remoteComponentName = componentName+ remoteEndpoint.toString().replaceAll("[/:]","" );
    logger.debug("remoteComponentName: {}", remoteComponentName);
    matcherProtocolCamelConfigurator.addCamelComponentForWonNodeBrokerForTopics(remoteEndpoint,
                                                                                remoteComponentName
    );
    matcherProtocolCamelConfigurator.addRemoteTopicListeners(endpoints, remoteEndpoint);
  }

  public synchronized void addRemoteTopicListeners(final Set<String> endpoints, final URI wonNodeUri, final
  KeyManager km, final TrustManager tm)
    throws CamelConfigurationFailedException {
    URI remoteEndpoint = activeMQService.getBrokerEndpoint(wonNodeUri);
    String remoteComponentName = componentName+ remoteEndpoint.toString().replaceAll("[/:]","" );
    logger.debug("remoteComponentName: {}", remoteComponentName);
    matcherProtocolCamelConfigurator.addCamelComponentForWonNodeBrokerForTopics(remoteEndpoint,
                                                                                remoteComponentName, km, tm
    );
    matcherProtocolCamelConfigurator.addRemoteTopicListeners(endpoints, remoteEndpoint);
  }

  @Override
  public URI getBrokerUri(URI resourceUri) throws NoSuchConnectionException {
    return activeMQService.getBrokerEndpoint(resourceUri);
  }

  @Override
  public ActiveMQService getActiveMQService() {
    return activeMQService;
  }

  @Override
  public void setActiveMQService(ActiveMQService activeMQService) {
    this.activeMQService = (MatcherActiveMQService) activeMQService;
  }

  @Override
  public CamelConfigurator getProtocolCamelConfigurator() {
    return this.matcherProtocolCamelConfigurator;

  }

  public void setComponentName(final String componentName) {
    this.componentName = componentName;
  }


  public void setMatcherProtocolCamelConfigurator(NeedProtocolCamelConfigurator matcherProtocolCamelConfigurator) {
    this.matcherProtocolCamelConfigurator = (MatcherProtocolCamelConfigurator) matcherProtocolCamelConfigurator;
  }


  public void setKeyStoreService(KeyStoreService keyStoreService) {
    this.keyStoreService = keyStoreService;
  }

  public void setTrustStoreService(TrustStoreService trustStoreService) {
    this.trustStoreService = trustStoreService;
  }

  public KeyStoreService getKeyStoreService() {
    return keyStoreService;
  }

  public TrustStoreService getTrustStoreService() {
    return trustStoreService;
  }
}
