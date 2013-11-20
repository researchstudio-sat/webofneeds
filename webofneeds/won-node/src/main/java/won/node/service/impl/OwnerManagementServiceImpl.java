package won.node.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.OwnerManagementService;

import java.util.List;
import java.util.UUID;

/**
 * User: sbyim
 * Date: 11.11.13
 */
public class OwnerManagementServiceImpl implements OwnerManagementService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OwnerApplicationRepository ownerApplicatonRepository;

    @Autowired
    private QueueManagementServiceImpl queueManagementService;

    @Override
    public String registerOwnerApplication() {
        UUID ownerApplicationId = UUID.randomUUID();  //TODO: review when looking for security issues
        logger.info("ownerApplicationId: "+ownerApplicationId.toString() );
        OwnerApplication ownerApplication = new OwnerApplication();
        ownerApplication.setOwnerApplicationId(ownerApplicationId.toString());
        logger.info("ownerApplicationId: "+ownerApplication.getOwnerApplicationId().toString() );
        ownerApplication = ownerApplicatonRepository.saveAndFlush(ownerApplication);
        List<String> queueNames = queueManagementService.generateQueueNamesForOwnerApplication(ownerApplication);
        ownerApplication.setQueueNames(queueNames);
        ownerApplication = ownerApplicatonRepository.saveAndFlush(ownerApplication);
        return ownerApplicationId.toString();

    }


    public void setQueueManagementService(QueueManagementServiceImpl queueManagementService) {
        this.queueManagementService = queueManagementService;
    }
}
