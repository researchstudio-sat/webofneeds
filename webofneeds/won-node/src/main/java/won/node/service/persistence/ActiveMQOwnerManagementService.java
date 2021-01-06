package won.node.service.persistence;

import java.util.List;
import won.protocol.model.OwnerApplication;
import won.protocol.service.OwnerManagementService;

public interface ActiveMQOwnerManagementService extends OwnerManagementService {
    List<String> generateQueueNamesForOwnerApplication(OwnerApplication ownerApplication);

    String generateQueueNameForOwnerApplicationId(String ownerApplicationId);

    String generateCamelEndpointNameForQueueName(String ownerApplicationOutQueue);

    String generateOwnerApplicationIdForQueueName(String ownerApplicationOutQueue);

    String sanitizeQueueNameForOwnerApplication(OwnerApplication ownerApplication, String queueName);

    String sanitizeQueueNameForOwnerApplicationId(String queueName,
                    String ownerApplicationId);

    boolean existsCamelEndpointForOwnerApplicationQueue(String queueName);
}
