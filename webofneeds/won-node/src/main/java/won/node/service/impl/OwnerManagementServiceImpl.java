package won.node.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.ApplicationManagementService;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * User: sbyim Date: 11.11.13
 */
public class OwnerManagementServiceImpl implements ApplicationManagementService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private OwnerApplicationRepository ownerApplicatonRepository;
    @Autowired
    private QueueManagementServiceImpl queueManagementService;

    @Override
    public String registerOwnerApplication(String ownerApplicationId) {
        logger.debug("ownerApplicationId: " + ownerApplicationId.toString());
        if (ownerApplicatonRepository.findByOwnerApplicationIdForUpdate(ownerApplicationId).isEmpty()) {
            logger.info("Registering owner application for the first time with id: {}", ownerApplicationId);
            OwnerApplication ownerApplication = new OwnerApplication();
            ownerApplication.setOwnerApplicationId(ownerApplicationId.toString());
            ownerApplication = ownerApplicatonRepository.save(ownerApplication);
            List<String> queueNames = queueManagementService.generateQueueNamesForOwnerApplication(ownerApplication);
            ownerApplication.setQueueNames(queueNames);
            ownerApplication = ownerApplicatonRepository.save(ownerApplication);
            return ownerApplicationId;
        } else {
            logger.info("Registering already known owner application with id: {}", ownerApplicationId);
            return ownerApplicationId;
        }
    }

    public void setQueueManagementService(QueueManagementServiceImpl queueManagementService) {
        this.queueManagementService = queueManagementService;
    }
}
