package won.protocol.repository;

import java.net.URI;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

import won.protocol.model.BAState;

/**
 * User: Danijel Date: 28.5.14.
 */
public interface BAStateRepository extends WonRepository<BAState> {
    List<BAState> findByCoordinatorURIAndParticipantURIAndSocketTypeURI(URI coordinatorUri, URI participantURI,
                    final URI socketURI);

    @Query("select baStateURI from BAState")
    List<URI> getAllBAStatesURIs();
}
