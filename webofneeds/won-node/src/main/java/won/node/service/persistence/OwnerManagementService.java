package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.protocol.model.OwnerApplication;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.ApplicationManagementService;

/**
 * User: sbyim Date: 11.11.13
 */
@Component
public class OwnerManagementService implements ApplicationManagementService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private OwnerApplicationRepository ownerApplicationRepository;

    @Override
    public String registerOwnerApplication(String ownerApplicationId) {
        logger.debug("ownerApplicationId: " + ownerApplicationId.toString());
        if (ownerApplicationRepository.findByOwnerApplicationIdForUpdate(ownerApplicationId).isEmpty()) {
            logger.info("Registering owner application for the first time with id: {}", ownerApplicationId);
            OwnerApplication ownerApplication = new OwnerApplication();
            ownerApplication.setOwnerApplicationId(ownerApplicationId.toString());
            ownerApplication = ownerApplicationRepository.save(ownerApplication);
            List<String> queueNames = generateQueueNamesForOwnerApplication(ownerApplication);
            ownerApplication.setQueueNames(queueNames);
            ownerApplication = ownerApplicationRepository.save(ownerApplication);
            return ownerApplicationId;
        } else {
            logger.info("Registering already known owner application with id: {}", ownerApplicationId);
            return ownerApplicationId;
        }
    }

    public List<String> generateQueueNamesForOwnerApplication(OwnerApplication ownerApplication) {
        logger.debug(ownerApplication.getOwnerApplicationId());
        List<String> queueNames = new ArrayList<>();
        queueNames.add("activemq" + ":queue:OwnerProtocol.Out." + ownerApplication.getOwnerApplicationId());
        ownerApplication.setQueueNames(queueNames);
        return ownerApplication.getQueueNames();
    }

    public String getEndpointForMessage(String methodName, String ownerApplicationID) {
        OwnerApplication ownerApplication = ownerApplicationRepository.findOneByOwnerApplicationId(ownerApplicationID)
                        .get();
        List<String> queueNames = ownerApplication.getQueueNames();
        String endpoint = "";
        for (int i = 0; i < queueNames.size(); i++) {
            endpoint = queueNames.get(i);
            if (queueNames.get(i).contains(methodName)) {
                break;
            }
        }
        return endpoint;
    }
}
