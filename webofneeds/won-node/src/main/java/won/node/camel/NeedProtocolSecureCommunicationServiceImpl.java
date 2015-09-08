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

package won.node.camel;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.KeyStoreService;
import won.cryptography.service.TrustStoreService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.*;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

/**
 * User: syim
 * Date: 27.01.14
 */

public class NeedProtocolSecureCommunicationServiceImpl implements NeedProtocolCommunicationService {

    @Autowired
    private NeedProtocolCamelConfigurator needProtocolCamelConfigurator;

    @Autowired
    private ActiveMQService activeMQService;

    private KeyStoreService keyStoreService;
    private TrustStoreService trustStoreService;

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    public synchronized CamelConfiguration configureCamelEndpoint(URI wonNodeUri) throws Exception {

        //TODO check known (registered with km and tm) - i.e. if broker name is known-configured - do nothing, if not,
        // do registration (just POST "?register-node:wonNodeUri" to remote node's wonNodeUri to obtain its certificate
        // and present yours web-id certificate - TODO the server should additionally do web-id verification)
        //TODO when doing this, for this endpoint i can configutre to use trust manager that trusts ONLY the
        // certificate under the alias wonNodeURI (or its host part since it's the server's certificate) -
        // by this the client makes sure that it sends messages only to the wonNodeUri server for this
        // endpoint, and not to any other, even among trusted servers, server.

        String needProtocolQueueName;
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        logger.debug("ensuring camel is configured for remote wonNodeUri", new Object[]{wonNodeUri});

        //TODO getBroker endpoint will do GET on wonNodeUri, since it is possible that we don't know the
        //server we should apply TOFU trust here, and don't supply our own certificate
        URI remoteNodeBrokerUri = activeMQService.getBrokerEndpoint(wonNodeUri);

        if (needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(remoteNodeBrokerUri)!=null){
            logger.debug("broker component name is already known");
            camelConfiguration.setEndpoint(needProtocolCamelConfigurator.getEndpoint(remoteNodeBrokerUri));
            camelConfiguration.setBrokerComponentName(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(remoteNodeBrokerUri));
            //HINT: we may have to handle routes that were shut down automatically after a timeout here...
        } else{
            logger.debug("broker component name unknown - setting up a new component for the remote broker");
            URI resourceUri;
            URI brokerUri;

            resourceUri = wonNodeUri;
            brokerUri = remoteNodeBrokerUri;

            needProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
            // register with remote node in order to exchange certificates if necessary. IF the same trust strategy will
            // be used when doing GET on won resource, we probably don't need this separate register for node
            registerNodeAtRemoteNode(wonNodeUri.toString());
            // initialize key and trust managers and pass them to configuration
            String keyAlias = keyStoreService.getDefaultAlias();
            //TODO handle password
            KeyManager km = CryptographyUtils.initializeKeyManager(keyStoreService, "temp", keyAlias);
            TrustManager tm = CryptographyUtils.initializeTrustManager(trustStoreService, wonNodeUri.toString());
            String endpoint = needProtocolCamelConfigurator.configureCamelEndpointForNeedUri(resourceUri, brokerUri,
                                                                                             needProtocolQueueName,
                                                                                             km, tm);
            camelConfiguration.setEndpoint(endpoint);
            camelConfiguration.setBrokerComponentName(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            ActiveMQComponent activeMQComponent = (ActiveMQComponent)needProtocolCamelConfigurator.getCamelContext().getComponent(needProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            logger.info("ActiveMQ Service Status : {}",activeMQComponent.getStatus().toString());
            activeMQComponent.start();
        }
        return camelConfiguration;
    }

    private void registerNodeAtRemoteNode(String remoteNodeUri) throws Exception {

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
    public URI getBrokerUri(URI resourceUri) throws NoSuchConnectionException {
        return activeMQService.getBrokerEndpoint(resourceUri);
    }

    @Override
    public ActiveMQService getActiveMQService() {
        return activeMQService;
    }


    @Override
    public void setActiveMQService(ActiveMQService activeMQService) {
        this.activeMQService = activeMQService;
    }


    @Override
    public CamelConfigurator getProtocolCamelConfigurator() {
        return needProtocolCamelConfigurator;
    }

    public void setKeyStoreService(KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }

    public void setTrustStoreService(TrustStoreService trustStoreService) {
        this.trustStoreService = trustStoreService;
    }
}
