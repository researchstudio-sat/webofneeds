package won.protocol.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import won.protocol.model.BAState;
import won.protocol.model.Facet;

import java.net.URI;
import java.util.List;

/**
 * User: Danijel
 * Date: 28.5.14.
 */
public interface BAStateRepository extends WonRepository<BAState>
{

  List<BAState> findByCoordinatorURIAndParticipantURIAndFacetTypeURI(URI coordinatorUri, URI participantURI, final URI facetURI);

  @Query("select baStateURI from BAState")
  List<URI> getAllBAStatesURIs();
}
