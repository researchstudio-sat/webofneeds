package won.protocol.repository;

import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import won.protocol.model.Lock;

public interface LockRepository extends CrudRepository<Lock, Long> {
    @org.springframework.data.jpa.repository.Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lock from Lock lock where lock.name='ownerapplication'")
    Lock getOwnerapplicationLock();
}
