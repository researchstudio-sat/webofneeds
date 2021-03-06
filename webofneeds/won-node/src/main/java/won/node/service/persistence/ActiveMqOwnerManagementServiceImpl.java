package won.node.service.persistence;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import won.protocol.model.Lock;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.LockRepository;
import won.protocol.repository.OwnerApplicationRepository;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * User: sbyim Date: 11.11.13
 */
@org.springframework.stereotype.Component
public class ActiveMqOwnerManagementServiceImpl implements ActiveMQOwnerManagementService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private OwnerApplicationRepository ownerApplicationRepository;
    @Autowired
    private LockRepository lockRepository;
    @Autowired
    private CamelContext camelContext;

    @Override
    @Transactional
    public String registerOwnerApplication(String ownerApplicationId) {
        logger.debug("about to register ownerApplicationId: {}", ownerApplicationId);
        Lock lock = lockRepository.getOwnerapplicationLock();
        Optional<OwnerApplication> ownerApplication = ownerApplicationRepository
                        .findOneByOwnerApplicationId(ownerApplicationId);
        if (!ownerApplication.isPresent()) {
            logger.info("Registering owner application for the first time with id: {}", ownerApplicationId);
            OwnerApplication newOwnerApplication = new OwnerApplication();
            newOwnerApplication.setOwnerApplicationId(ownerApplicationId.toString());
            newOwnerApplication = ownerApplicationRepository.save(newOwnerApplication);
            List<String> queueNames = generateQueueNamesForOwnerApplication(newOwnerApplication);
            newOwnerApplication.setQueueNames(queueNames);
            newOwnerApplication = ownerApplicationRepository.save(newOwnerApplication);
            ownerApplication = Optional.of(newOwnerApplication);
        } else {
            logger.info("Registering already known owner application with id: {}", ownerApplicationId);
        }
        addCamelEndpointsForOwnerApplication(ownerApplication.get());
        return ownerApplicationId;
    }

    private synchronized void addCamelEndpointsForOwnerApplication(OwnerApplication ownerApplication) {
        List<String> componentNames = camelContext.getComponentNames();
        Component activemqComponent = (Component) camelContext.getComponent("activemq");
        ((ActiveMQComponent) activemqComponent).setPreserveMessageQos(true);
        for (String queueName : ownerApplication.getQueueNames()) {
            queueName = sanitizeQueueNameForOwnerApplication(ownerApplication, queueName);
            Endpoint existingQueueEndpoint = camelContext.hasEndpoint(queueName);
            if (existingQueueEndpoint != null) {
                logger.debug("endpoint '{}' already present in camel context", queueName);
            } else {
                try {
                    Endpoint newQueueEndpoint = activemqComponent.createEndpoint(queueName);
                    camelContext.addEndpoint(queueName, newQueueEndpoint);
                    logger.debug("added camel endpoint '{}'", queueName);
                } catch (Exception e) {
                    logger.warn("Could not register camel endpoint for activeMQ queue {}", queueName, e);
                }
            }
        }
    }

    @Override
    public List<String> generateQueueNamesForOwnerApplication(OwnerApplication ownerApplication) {
        List<String> queueNames = new ArrayList<>();
        String ownerApplicationId = ownerApplication.getOwnerApplicationId();
        queueNames.add(generateQueueNameForOwnerApplicationId(ownerApplicationId));
        return queueNames;
    }

    @Override
    public String generateQueueNameForOwnerApplicationId(String ownerApplicationId) {
        return "activemq:queue:OwnerProtocol.Out." + ownerApplicationId;
    }

    @Override
    public String generateCamelEndpointNameForQueueName(String ownerApplicationOutQueue) {
        if (ownerApplicationOutQueue == null
                        || !ownerApplicationOutQueue.startsWith("OwnerProtocol.Out.")) {
            throw new IllegalArgumentException("Not an owner queue name: " + ownerApplicationOutQueue);
        }
        return "activemq:queue:" + ownerApplicationOutQueue;
    }

    @Override
    public String generateOwnerApplicationIdForQueueName(String ownerApplicationOutQueue) {
        if (ownerApplicationOutQueue == null
                        || !ownerApplicationOutQueue.startsWith("OwnerProtocol.Out.")) {
            throw new IllegalArgumentException("Not an owner queue name: " + ownerApplicationOutQueue);
        }
        return ownerApplicationOutQueue.substring("OwnerProtocol.Out.".length());
    }

    /**
     * Checks if the given queue name is exactly equal to the ownerApplicationId,
     * and returns the String "activemq:queue:OwnerProtocol.Out." + queueName,
     * otherwise, just returns the queue name
     * 
     * @return the sanitized queue name
     */
    @Override
    public String sanitizeQueueNameForOwnerApplication(OwnerApplication ownerApplication,
                    String queueName) {
        String ownerApplicationId = ownerApplication.getOwnerApplicationId();
        return sanitizeQueueNameForOwnerApplicationId(queueName, ownerApplicationId);
    }

    @Override
    public String sanitizeQueueNameForOwnerApplicationId(String queueName,
                    String ownerApplicationId) {
        if (ownerApplicationId != null && ownerApplicationId.trim().equals(queueName.trim())) {
            String newQueueName = generateQueueNameForOwnerApplicationId(ownerApplicationId);
            if (logger.isDebugEnabled()) {
                logger.debug("automatically converting ownerApplicationId {} to queue name {}",
                                ownerApplicationId,
                                newQueueName);
            }
            return newQueueName;
        }
        return queueName;
    }

    /**
     * @param ownerAppliation
     * @param queueName
     * @return
     */
    @Override
    public boolean existsCamelEndpointForOwnerApplicationQueue(String queueName) {
        return (camelContext.hasEndpoint(queueName) != null);
    }
}
