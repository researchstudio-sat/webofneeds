package won.matcher.protocol.impl;

import org.apache.camel.component.activemq.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.service.RegistrationRestClientHttps;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.*;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Set;

/**
 * User: ypanchenko Date: 02.09.2015
 */
public class MatcherProtocolCommunicationServiceImpl implements MatcherProtocolCommunicationService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private RegistrationRestClientHttps registrationClient;
    private MatcherProtocolCamelConfigurator matcherProtocolCamelConfigurator;
    private MatcherActiveMQService activeMQService;

    public void setRegistrationClient(final RegistrationRestClientHttps registrationClient) {
        this.registrationClient = registrationClient;
    }

    @Override
    public synchronized CamelConfiguration configureCamelEndpoint(URI nodeUri, String startingEndpoint)
                    throws Exception {
        String matcherProtocolQueueName;
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        URI atomBrokerUri = activeMQService.getBrokerEndpoint(nodeUri);
        if (matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(atomBrokerUri) != null) {
            String endpoint = matcherProtocolCamelConfigurator.getEndpoint(nodeUri);
            if (endpoint != null) {
                camelConfiguration.setEndpoint(endpoint);
            } else {
                matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint, atomBrokerUri);
                matcherProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(nodeUri);
                // register with remote node. If at some point the same trust strategy will
                // be used when doing GET on won resource, we don't need this separate register
                // step for node
                registrationClient.register(nodeUri.toString());
                endpoint = matcherProtocolCamelConfigurator.configureCamelEndpointForAtomUri(nodeUri, atomBrokerUri,
                                matcherProtocolQueueName);
                camelConfiguration.setEndpoint(endpoint);
            }
            camelConfiguration.setBrokerComponentName(
                            matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(atomBrokerUri));
        } else {
            URI resourceUri = nodeUri;
            URI brokerUri = atomBrokerUri;
            matcherProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
            camelConfiguration.setEndpoint(matcherProtocolCamelConfigurator
                            .configureCamelEndpointForAtomUri(resourceUri, brokerUri, matcherProtocolQueueName));
            matcherProtocolCamelConfigurator.addRouteForEndpoint(startingEndpoint, brokerUri);
            camelConfiguration.setBrokerComponentName(
                            matcherProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            ActiveMQComponent activeMQComponent = (ActiveMQComponent) matcherProtocolCamelConfigurator.getCamelContext()
                            .getComponent(matcherProtocolCamelConfigurator
                                            .getBrokerComponentNameWithBrokerUri(brokerUri));
            activeMQComponent.setTransacted(false);
            activeMQComponent.setUsePooledConnection(true);
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

    public void setMatcherProtocolCamelConfigurator(AtomProtocolCamelConfigurator matcherProtocolCamelConfigurator) {
        this.matcherProtocolCamelConfigurator = (MatcherProtocolCamelConfigurator) matcherProtocolCamelConfigurator;
    }
}
