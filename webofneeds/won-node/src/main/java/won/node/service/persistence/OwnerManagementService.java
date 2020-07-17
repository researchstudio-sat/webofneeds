package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.ApplicationManagementService;

/**
 * User: sbyim Date: 11.11.13
 */
@org.springframework.stereotype.Component
public class OwnerManagementService implements ApplicationManagementService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private OwnerApplicationRepository ownerApplicationRepository;
    @Autowired
    private CamelContext camelContext;

    @Override
    public String registerOwnerApplication(String ownerApplicationId) {
        logger.debug("ownerApplicationId: " + ownerApplicationId.toString());
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
        for (String queueName : ownerApplication.getQueueNames()) {
            Endpoint existingQueueEndpoint = camelContext.hasEndpoint(queueName);
            if (existingQueueEndpoint != null) {
                logger.debug("endpoint '{}' already present in camel context", queueName);
            } else {
                try {
                    Endpoint newQueueEndpoint = activemqComponent.createEndpoint(queueName);
                    camelContext.addEndpoint(queueName, newQueueEndpoint);
                } catch (Exception e) {
                    logger.warn("Could not register camel endpoint for activeMQ queue {}", queueName, e);
                }
            }
        }
    }

    public List<String> generateQueueNamesForOwnerApplication(OwnerApplication ownerApplication) {
        List<String> queueNames = new ArrayList<>();
        queueNames.add("activemq" + ":queue:OwnerProtocol.Out." + ownerApplication.getOwnerApplicationId());
        return queueNames;
    }

    public boolean existsCamelEndpointForOwnerApplicationQueue(String queueName) {
        return (camelContext.hasEndpoint(queueName) != null);
    }
}
