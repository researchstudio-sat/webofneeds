package won.protocol.repository;

import java.net.URI;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import won.protocol.model.MessageContainer;

public interface MessageContainerRepository extends WonRepository<MessageContainer> {
    Optional<MessageContainer> findOneByParentUri(URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from MessageContainer c where c.parentUri = :parentUri")
    Optional<MessageContainer> findOneByParentUriForUpdate(@Param("parentUri") URI parentUri);
}
