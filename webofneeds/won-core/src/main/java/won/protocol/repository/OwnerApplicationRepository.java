package won.protocol.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import won.protocol.model.OwnerApplication;

/**
 * User: sbyim Date: 11.11.13
 */
public interface OwnerApplicationRepository extends WonRepository<OwnerApplication> {
    List<OwnerApplication> findByOwnerApplicationId(String ownerApplicationId);

    Optional<OwnerApplication> findOneByOwnerApplicationId(String ownerApplicationId);

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select app from OwnerApplication app where app.ownerApplicationId = :ownerApplicationId")
    List<OwnerApplication> findByOwnerApplicationIdForUpdate(@Param("ownerApplicationId") String ownerApplicationId);
}
