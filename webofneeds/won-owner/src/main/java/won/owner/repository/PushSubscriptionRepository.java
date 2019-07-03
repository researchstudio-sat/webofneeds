package won.owner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import won.owner.model.PushSubscription;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {
}
