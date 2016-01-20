package won.node.service.impl;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.ApplicationManagementService;

import java.util.List;
import java.util.UUID;

/**
 * User: sbyim
 * Date: 11.11.13
 */
public class OwnerManagementServiceImpl implements ApplicationManagementService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OwnerApplicationRepository ownerApplicatonRepository;

    @Autowired
    private QueueManagementServiceImpl queueManagementService;

    @Override
    public String  registerOwnerApplication(Exchange exchange) {
        UUID ownerApplicationId = UUID.randomUUID();  //TODO: owner application id generation shall be reviewed.
        return registerOwnerApplication(ownerApplicationId.toString());
    }

    @Override
    public String registerOwnerApplication(String ownerApplicationId) {

        logger.debug("ownerApplicationId: "+ownerApplicationId.toString() );

        if (ownerApplicatonRepository.findByOwnerApplicationId(ownerApplicationId).isEmpty()) {
            OwnerApplication ownerApplication = new OwnerApplication();
            ownerApplication.setOwnerApplicationId(ownerApplicationId.toString());
            logger.debug("ownerApplicationId: " + ownerApplication.getOwnerApplicationId().toString());
            ownerApplication = ownerApplicatonRepository.save(ownerApplication);
            List<String> queueNames = queueManagementService.generateQueueNamesForOwnerApplication(ownerApplication);
            ownerApplication.setQueueNames(queueNames);
            ownerApplication = ownerApplicatonRepository.save(ownerApplication);
            return ownerApplicationId.toString();
        } else {
            logger.error("Registration failed: owner with id {} is already registered", ownerApplicationId);
            return ownerApplicationId;
        }

    }

    public void setQueueManagementService(QueueManagementServiceImpl queueManagementService) {
        this.queueManagementService = queueManagementService;
    }
}
