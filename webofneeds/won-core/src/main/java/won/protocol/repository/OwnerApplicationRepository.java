package won.protocol.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import won.protocol.model.OwnerApplication;

import javax.persistence.LockModeType;
import java.util.List;

/**
 * User: sbyim Date: 11.11.13
 */
public interface OwnerApplicationRepository extends WonRepository<OwnerApplication> {
  List<OwnerApplication> findByOwnerApplicationId(String ownerApplicationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select app from OwnerApplication app where app.ownerApplicationId = :ownerApplicationId")
  List<OwnerApplication> findByOwnerApplicationIdForUpdate(@Param("ownerApplicationId") String ownerApplicationId);
}
