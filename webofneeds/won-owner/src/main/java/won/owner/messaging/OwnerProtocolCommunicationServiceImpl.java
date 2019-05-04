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
package won.owner.messaging;

import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.cryptography.service.CryptographyService;
import won.cryptography.service.RegistrationClient;
import won.cryptography.service.RegistrationRestClientHttps;
import won.cryptography.service.keystore.KeyStoreService;
import won.cryptography.ssl.AliasFromFingerprintGenerator;
import won.cryptography.ssl.AliasGenerator;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.ActiveMQService;
import won.protocol.jms.CamelConfiguration;
import won.protocol.jms.MessagingService;
import won.protocol.jms.OwnerProtocolCamelConfigurator;
import won.protocol.jms.OwnerProtocolCommunicationService;
import won.protocol.model.Connection;
import won.protocol.model.Atom;
import won.protocol.model.WonNode;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.WonNodeRepository;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.LoggingUtils;

/**
 * User: syim Date: 27.01.14
 */
public class OwnerProtocolCommunicationServiceImpl implements OwnerProtocolCommunicationService {
    @Autowired
    private OwnerProtocolCamelConfiguratorImpl ownerProtocolCamelConfigurator;
    private ActiveMQService activeMQService;
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private WonNodeRepository wonNodeRepository;
    @Autowired
    private CryptographyService cryptographyService;
    @Autowired
    private KeyStoreService keyStoreService;
    private AliasGenerator aliasGenerator = new AliasFromFingerprintGenerator();
    // can also be autowired
    private RegistrationClient registrationClient;
    public static final String REMOTE_INCOMING_QUEUE_PREFIX = ":queue:OwnerProtocol.Out.";
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public void setRegistrationClient(final RegistrationRestClientHttps registrationClient) {
        this.registrationClient = registrationClient;
    }

    public synchronized boolean isRegisteredWithWonNode(URI wonNodeURI) {
        try {
            String ownerApplicationId = calculateOwnerApplicationIdFromOwnerCertificate();
            logger.debug("using ownerApplicationId: {}", ownerApplicationId);
            WonNode wonNode = wonNodeRepository.findOneByWonNodeURIAndOwnerApplicationID(wonNodeURI,
                            ownerApplicationId);
            return ownerProtocolCamelConfigurator.getCamelContext().getComponent(wonNode.getBrokerComponent()) != null;
        } catch (Exception e) {
            logger.info("error while checking if we are registered with WoN node " + wonNodeURI, e);
        }
        return false;
    }

    /**
     * Registers the owner application at a won node. Owner Id is typically his Key
     * ID (lower 64 bits of the owner public key fingerprint). Unless there is a
     * collision of owner ids on the node - then the owner can assign another id...
     *
     * @return ownerApplicationId
     * @throws Exception
     */
    public synchronized void register(URI wonNodeURI, MessagingService messagingService) throws Exception {
        CamelConfiguration camelConfiguration = null;
        logger.debug("setting up communication with won node {} ", wonNodeURI);
        String ownerApplicationId = calculateOwnerApplicationIdFromOwnerCertificate();
        logger.debug("using ownerApplicationId: {}", ownerApplicationId);
        WonNode wonNode = wonNodeRepository.findOneByWonNodeURIAndOwnerApplicationID(wonNodeURI, ownerApplicationId);
        if (wonNode != null) {
            // we think we are registered. Try to connect. If that fails, we'll try to re
            // register further below
            try {
                logger.debug("we're already registered. Connecting with WoN node: " + wonNodeURI);
                configureCamelEndpoint(wonNodeURI, ownerApplicationId);
                configureRemoteEndpointForOwnerApplication(ownerApplicationId,
                                getProtocolCamelConfigurator().getEndpoint(wonNodeURI));
                logger.debug("connected with WoN node: " + wonNodeURI);
                return;
            } catch (Exception e) {
                LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e,
                                "We thought we were already registerd, but connecting to {} failed With an exception. Trying to re-register. ",
                                wonNodeURI);
            }
            // we'll try to re-register now, see below. This is necessary if the WoN node
            // forgets about us for whatever
            // reason.
        }
        logger.info("we're not yet registered. Registering with WoN node {} under ownerApplicationId {}", wonNodeURI,
                        ownerApplicationId);
        String nodeGeneratedOwnerApplicationId = registrationClient.register(wonNodeURI.toString());
        if (!ownerApplicationId.equals(nodeGeneratedOwnerApplicationId)) {
            throw new java.lang.IllegalStateException("WoN node " + wonNodeURI
                            + " generated an ownerApplicationId that differs from" + " ours. Node generated: "
                            + nodeGeneratedOwnerApplicationId + ", we " + "generated: " + ownerApplicationId);
        }
        logger.debug("registered with WoN node: " + wonNodeURI + ",  ownerappID: " + ownerApplicationId);
        camelConfiguration = configureCamelEndpoint(wonNodeURI, ownerApplicationId);
        storeWonNode(ownerApplicationId, camelConfiguration, wonNodeURI);
        configureRemoteEndpointForOwnerApplication(ownerApplicationId,
                        getProtocolCamelConfigurator().getEndpoint(wonNodeURI));
        logger.info("connected with WoN node: : " + wonNodeURI);
    }

    private String calculateOwnerApplicationIdFromOwnerCertificate() throws CertificateException {
        Certificate cert = keyStoreService.getCertificate(cryptographyService.getDefaultPrivateKeyAlias());
        return aliasGenerator.generateAlias((X509Certificate) cert);
    }

    // TODO this is messy, has to be improved, maybe endpoints should be obtained in
    // the same step as registration,
    // e.g. the register call returns not only application id, but also the
    // endpoints...
    private void configureRemoteEndpointForOwnerApplication(String ownerApplicationID, String remoteEndpoint)
                    throws CamelConfigurationFailedException, ExecutionException, InterruptedException {
        getProtocolCamelConfigurator().addRemoteQueueListener(REMOTE_INCOMING_QUEUE_PREFIX + ownerApplicationID,
                        URI.create(remoteEndpoint));
        // TODO: some checks needed to assure that the application is configured
        // correctly.
        // todo this method should return routes
    }

    /**
     * Stores the won node information, possibly overwriting existing data.
     *
     * @param ownerApplicationId
     * @param camelConfiguration
     * @param wonNodeURI
     * @return
     * @throws NoSuchConnectionException
     */
    public WonNode storeWonNode(String ownerApplicationId, CamelConfiguration camelConfiguration, URI wonNodeURI)
                    throws NoSuchConnectionException {
        WonNode wonNode = DataAccessUtils.loadWonNode(wonNodeRepository, wonNodeURI);
        if (wonNode == null) {
            wonNode = new WonNode();
        }
        wonNode.setOwnerApplicationID(ownerApplicationId);
        wonNode.setOwnerProtocolEndpoint(camelConfiguration.getEndpoint());
        wonNode.setWonNodeURI(wonNodeURI);
        wonNode.setBrokerURI(getBrokerUri(wonNodeURI));
        wonNode.setBrokerComponent(camelConfiguration.getBrokerComponentName());
        wonNode.setStartingComponent(getProtocolCamelConfigurator().getStartingEndpoint(wonNodeURI));
        wonNodeRepository.save(wonNode);
        logger.debug("setting starting component {}", wonNode.getStartingComponent());
        return wonNode;
    }

    public final synchronized CamelConfiguration configureCamelEndpoint(URI wonNodeUri, String ownerId)
                    throws Exception {
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        URI brokerURI = activeMQService.getBrokerEndpoint(wonNodeUri);
        String ownerProtocolQueueName;
        List<WonNode> wonNodeList = wonNodeRepository.findByWonNodeURI(wonNodeUri);
        // OwnerProtocolCamelConfigurator ownerProtocolCamelConfigurator =
        // camelConfiguratorFactory.createCamelConfigurator(methodName);
        logger.debug("configuring camel endpoint");
        if (ownerProtocolCamelConfigurator.getBrokerComponentName(brokerURI) != null
                        && ownerProtocolCamelConfigurator.getEndpoint(wonNodeUri) != null) {
            logger.debug("wonNode known");
            WonNode wonNode = wonNodeList.get(0);
            // brokerURI = wonNode.getBrokerURI();
            camelConfiguration.setEndpoint(wonNode.getOwnerProtocolEndpoint());
            if (ownerProtocolCamelConfigurator.getCamelContext()
                            .getComponent(wonNodeList.get(0).getBrokerComponent()) == null) {
                // camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.addCamelComponentForWonNodeBroker
                // (wonNode.getWonNodeURI(),brokerURI,wonNode.getOwnerApplicationID()));
                ownerProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(wonNodeUri);
                String endpoint = ownerProtocolCamelConfigurator.configureCamelEndpointForNodeURI(wonNodeUri, brokerURI,
                                ownerProtocolQueueName);
                camelConfiguration.setBrokerComponentName(
                                ownerProtocolCamelConfigurator.getBrokerComponentName(brokerURI));
                ownerProtocolCamelConfigurator.getCamelContext()
                                .getComponent(camelConfiguration.getBrokerComponentName())
                                .createEndpoint(camelConfiguration.getEndpoint());
                if (ownerProtocolCamelConfigurator.getCamelContext().getRoute(wonNode.getStartingComponent()) == null)
                    ownerProtocolCamelConfigurator.addRouteForEndpoint(null, wonNode.getWonNodeURI());
            }
        } else { // if unknown wonNode
            logger.debug("wonNode unknown");
            // TODO: brokerURI gets the node information already. so requesting node
            // information again for queuename would be duplicate
            ownerProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(wonNodeUri);
            String endpoint = ownerProtocolCamelConfigurator.configureCamelEndpointForNodeURI(wonNodeUri, brokerURI,
                            ownerProtocolQueueName);
            camelConfiguration.setEndpoint(endpoint);
            camelConfiguration.setBrokerComponentName(ownerProtocolCamelConfigurator.getBrokerComponentName(brokerURI));
            ownerProtocolCamelConfigurator.addRouteForEndpoint(null, wonNodeUri);
        }
        return camelConfiguration;
    }

    @Override
    public synchronized URI getWonNodeUriWithConnectionUri(URI connectionUri) throws NoSuchConnectionException {
        // TODO: make this more efficient
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionUri);
        URI atomURI = con.getAtomURI();
        Atom atom = atomRepository.findByAtomURI(atomURI).get(0);
        return atom.getWonNodeURI();
    }

    @Override
    public synchronized URI getWonNodeUriWithAtomUri(URI atomUri) throws NoSuchConnectionException {
        Atom atom = atomRepository.findByAtomURI(atomUri).get(0);
        return atom.getWonNodeURI();
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
    public OwnerProtocolCamelConfigurator getProtocolCamelConfigurator() {
        return ownerProtocolCamelConfigurator;
    }

    public void setCryptographyService(final CryptographyService cryptographyService) {
        this.cryptographyService = cryptographyService;
    }
}
