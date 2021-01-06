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
package won.node.activemq;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.ConsumerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import won.cryptography.ssl.AliasFromFingerprintGenerator;
import won.cryptography.ssl.AliasGenerator;

import java.lang.invoke.MethodHandles;
import java.security.cert.X509Certificate;
import won.node.service.persistence.OwnerManagementService;

/**
 * BrokerFilter implementation that authorizes consumers if their TLS
 * certificate digest matches the suffix of the queue name they want to listen
 * to.
 */
public class CertificateCheckingBrokerFilter extends BrokerFilter {
    private final String queueNamePrefixToCheck;
    private OwnerManagementService ownerManagementService;
    private final AliasGenerator aliasGenerator = new AliasFromFingerprintGenerator();
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public CertificateCheckingBrokerFilter(final Broker next, String queueNamePrefixToCheck,
                    OwnerManagementService ownerManagementService) {
        super(next);
        this.ownerManagementService = ownerManagementService;
        this.queueNamePrefixToCheck = queueNamePrefixToCheck;
    }

    @Override
    @Transactional
    public Subscription addConsumer(final ConnectionContext context, final ConsumerInfo info) throws Exception {
        assert info != null : "ConsumerInfo must not be null";
        assert context != null : "ConnectionContext must not be null";
        if (shouldCheck(info)) {
            boolean checkPassed;
            String ownerAppOutQueueName = info.getDestination().getPhysicalName();
            try {
                checkPassed = isOwnerAllowedToConsume(context, info);
            } catch (Exception e) {
                throw new SecurityException("could not perform access control check for consumer "
                                + info.getConsumerId() + " and destination " + info.getDestination());
            }
            if (!checkPassed)
                throw new SecurityException("consumer " + info.getConsumerId()
                                + " not allowed to consume from destination " + info.getDestination());
            synchronized (this) {
                if (!ownerManagementService
                                .existsCamelEndpointForOwnerApplicationQueue(
                                                ownerManagementService.generateCamelEndpointNameForQueueName(
                                                                ownerAppOutQueueName))) {
                    String ownerApplicationId = ownerManagementService
                                    .generateOwnerApplicationIdForQueueName(ownerAppOutQueueName);
                    ownerManagementService.registerOwnerApplication(ownerApplicationId);
                    logger.info("registered ownerapplication {} by connecting on activemq {}", ownerApplicationId,
                                    ownerAppOutQueueName);
                }
            }
        }
        logger.debug("consumer added. destination: {}, consumerId: {}", info.getDestination(), info.getConsumerId());
        return super.addConsumer(context, info);
    }

    /**
     * Owner id is defined as a sha3-224 digest of the owner's certificate , based
     * on the results of comparing the owner id in queue name and the provided
     * certificate fingerprint, the access to read from that queue can be granted or
     * denied.
     * 
     * @param context
     * @param info
     * @return
     */
    private boolean isOwnerAllowedToConsume(final ConnectionContext context, final ConsumerInfo info) {
        logger.debug("checking if consumer {} is allowed to consume {} ", info.getConsumerId(), info.getDestination());
        String ownerAppOutQueueName = info.getDestination().getPhysicalName();
        if (context.getConnectionState().getInfo().getTransportContext() instanceof X509Certificate[]) {
            X509Certificate ownerCert = ((X509Certificate[]) context.getConnectionState().getInfo()
                            .getTransportContext())[0];
            String certificateDigest = null;
            try {
                certificateDigest = aliasGenerator.generateAlias(ownerCert);
                logger.debug("digest value of certificate: {}", certificateDigest);
            } catch (Exception e) {
                new IllegalArgumentException("Could not calculate sha-1 of owner certificate", e);
            }
            String forOwnerId = ownerManagementService.generateOwnerApplicationIdForQueueName(ownerAppOutQueueName);
            logger.debug("owner id suffix of queue name: {}", forOwnerId);
            if (certificateDigest.equals(forOwnerId)) {
                logger.debug("allowing to consume");
                return true;
            }
            logger.info("denying message consumption to owner as public key hash does not equal owner id");
            return false;
        } else {
            logger.info("denying message consumption to owner transportContext is not an X.509 certificate");
            return false;
        }
    }

    private boolean shouldCheck(final ConsumerInfo info) {
        return info.getDestination().getPhysicalName().indexOf(queueNamePrefixToCheck) == 0;
    }
}
