package won.matcher.protocol.impl;

import java.net.URI;
import java.util.Set;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.cryptography.service.RegistrationRestClientHttps;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.ActiveMQService;
import won.protocol.jms.CamelConfiguration;
import won.protocol.jms.CamelConfigurator;
import won.protocol.jms.MatcherActiveMQService;
import won.protocol.jms.MatcherProtocolCamelConfigurator;
import won.protocol.jms.MatcherProtocolCommunicationService;
import won.protocol.jms.NeedProtocolCamelConfigurator;

/**
 * User: ypanchenko Date: 02.09.2015
 */
public class MatcherProtocolCommunicationServiceImpl implements MatcherProtocolCommunicationService {

    private RegistrationRestClientHttps registrationClient;

    private MatcherProtocolCamelConfigurator matcherProtocolCamelConfigurator;

    private MatcherActiveMQService activeMQService;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void setRegistrationClient(final RegistrationRestClientHttps registrationClient) {
        this.registrationClient = registrationClient;
    }

    @Override
    public synchronized CamelConfiguration configureCamelEndpoint(URI nodeUri, String startingEndpoint)
            throws Exception {
        String matcherProtocolQueueName;
        CamelConfiguration camelConfiguration = new CamelConfiguration();

        URI needBrokerUri = activeMQService.getBrokerEndpoint(nodeUri);

        if (matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(needBrokerUri) != null) {
            String endpoint = matcherProtocolCamelConfigurator.getEndpoint(nodeUri);
            if (endpoint != null) {
                camelConfiguration.setEndpoint(endpoint);
            } else {
                matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint, needBrokerUri);
                matcherProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(nodeUri);

                // register with remote node. If at some point the same trust strategy will
                // be used when doing GET on won resource, we don't need this separate register step for node
                registrationClient.register(nodeUri.toString());
                endpoint = matcherProtocolCamelConfigurator.configureCamelEndpointForNeedUri(nodeUri, needBrokerUri,
                        matcherProtocolQueueName);
                camelConfiguration.setEndpoint(endpoint);
            }
            camelConfiguration.setBrokerComponentName(
                    matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(needBrokerUri));

        } else {

            URI resourceUri = nodeUri;
            URI brokerUri = needBrokerUri;

            matcherProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
            camelConfiguration.setEndpoint(matcherProtocolCamelConfigurator
                    .configureCamelEndpointForNeedUri(resourceUri, brokerUri, matcherProtocolQueueName));
            matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint, brokerUri);
            camelConfiguration.setBrokerComponentName(
                    matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            ActiveMQComponent activeMQComponent = (ActiveMQComponent) matcherProtocolCamelConfigurator.getCamelContext()
                    .getComponent(matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            logger.info("ActiveMQ Service Status : {}", activeMQComponent.getStatus().toString());
            activeMQComponent.start();
        }
        return camelConfiguration;
    }

    @Override
    public synchronized Set<String> getMatcherProtocolOutTopics(URI wonNodeURI) {
        Set<String> matcherProtocolTopics = ((MatcherActiveMQService) activeMQService)
                .getMatcherProtocolTopicNamesWithResource(wonNodeURI);
        return matcherProtocolTopics;
    }

    @Override
    public synchronized void addRemoteTopicListeners(final Set<String> endpoints, final URI wonNodeUri)
            throws CamelConfigurationFailedException {

        try {
            registrationClient.register(wonNodeUri.toString());
            URI remoteEndpoint = activeMQService.getBrokerEndpoint(wonNodeUri);
            String remoteComponentName = matcherProtocolCamelConfigurator.setupBrokerComponentName(remoteEndpoint);
            logger.debug("remoteComponentName: {}", remoteComponentName);
            matcherProtocolCamelConfigurator.addCamelComponentForWonNodeBrokerForTopics(remoteEndpoint,
                    remoteComponentName);
            matcherProtocolCamelConfigurator.addRemoteTopicListeners(endpoints, remoteEndpoint);
        } catch (CamelConfigurationFailedException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("Error of security configuration for communication with " + wonNodeUri.toString());
            throw new CamelConfigurationFailedException(e);
        }

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

    public void setMatcherProtocolCamelConfigurator(NeedProtocolCamelConfigurator matcherProtocolCamelConfigurator) {
        this.matcherProtocolCamelConfigurator = (MatcherProtocolCamelConfigurator) matcherProtocolCamelConfigurator;
    }

}
