package won.node.facet.businessactivity.statemanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.protocol.model.BAState;
import won.protocol.repository.BAStateRepository;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * User: Danijel
 * Date: 28.5.14.
 */
public class JpaBasedBAStateManager implements BAStateManager
{
  @Autowired
  BAStateRepository stateRepository;


  @Override
  @Transactional(readOnly = true)
  public URI getStateForNeedUri(final URI coordinatorURI, final URI participantURI, final URI facetURI) {
    List<BAState> states = stateRepository.findByCoordinatorURIAndParticipantURIAndFacetTypeURI(coordinatorURI, participantURI, facetURI);
    Iterator<BAState> stateIterator = states.iterator();
    if(stateIterator.hasNext()) {
      return stateIterator.next().getBaStateURI();
    } else {
      return null;
    }
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public void setStateForNeedUri(final URI stateUri, final URI coordinatorURI, final URI participantURI, final URI facetURI) {
    BAState state = null;
    List<BAState> states = stateRepository.findByCoordinatorURIAndParticipantURIAndFacetTypeURI(coordinatorURI, participantURI, facetURI);
    Iterator<BAState> stateIterator = states.iterator();
    if(stateIterator.hasNext()) {
      state = stateIterator.next();
      if (stateIterator.hasNext()) {
        throw new IllegalStateException("found two states for same coordinator/participant/facet combination");
      }
    } else {
      state = new BAState();
    }
    state.setCoordinatorURI(coordinatorURI);
    state.setParticipantURI(participantURI);
    state.setBaStateURI(stateUri);
    state.setFacetTypeURI(facetURI);
    stateRepository.save(state);
  }

  public void setStateRepository(final BAStateRepository stateRepository) {
    this.stateRepository = stateRepository;
  }
}
