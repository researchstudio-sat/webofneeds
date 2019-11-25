package won.protocol.repository;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

import won.protocol.model.PendingConfirmation;

public interface PendingConfirmationRepository extends WonRepository<PendingConfirmation> {
    Set<PendingConfirmation> findAllByMessageContainerIdAndConfirmingMessageURIIn(Long id,
                    Collection<URI> confirmingMessageURIs);

    void deleteByMessageContainerIdAndConfirmingMessageURIIn(Long id, Collection<URI> confirmingMessageURIs);
}
