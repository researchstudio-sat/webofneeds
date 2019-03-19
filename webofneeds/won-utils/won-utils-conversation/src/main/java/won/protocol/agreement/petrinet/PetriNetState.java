package won.protocol.agreement.petrinet;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.imperial.pipe.animation.PetriNetAnimator;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;

public class PetriNetState {

    // the petri net
    private PetriNet petriNet;

    // the animator (allowing us to change the pn's state)
    private PetriNetAnimator petriNetAnimator;

    // The URI chosen by one of the participants for this petri net
    private URI petriNetURI;

    public PetriNetState(URI petrinetURI, PetriNet petriNet) {
        super();
        this.petriNetURI = petrinetURI;
        this.petriNet = petriNet;
        this.petriNetAnimator = new PetriNetAnimator(petriNet);
    }

    /**
     * Returns the set of URIs associatied with all marked places. Nothing is reported for places that are not
     * associatied with a URI.
     */
    public Set<URI> getMarkedPlaces() {
        return petriNet.getPlaces().stream().filter(place -> place.getNumberOfTokensStored() > 0)
                .map(place -> URI.create(place.getId())).collect(Collectors.toSet());
    }

    /**
     * Returns the set of URIs associatied with all enabled transitions. Nothing is reported for transitions that are
     * not associatied with a URI.
     */
    public Set<URI> getEnabledTransitions() {
        return petriNetAnimator.getEnabledTransitions().stream().map(t -> URI.create(t.getId()))
                .collect(Collectors.toSet());
    }

    public void fireTransition(URI transitionURI) {
        petriNetAnimator.getEnabledTransitions().stream().filter(t -> transitionURI.toString().equals(t.getId()))
                .forEach(petriNetAnimator::fireTransition);
    }

    public Set<URI> getPlaces() {
        return petriNet.getPlaces().stream().map(p -> URI.create(p.getId())).collect(Collectors.toSet());
    }

    public Set<URI> getTransitions() {
        return petriNet.getTransitions().stream().map(p -> URI.create(p.getId())).collect(Collectors.toSet());
    }

    public URI getPetriNetURI() {
        return petriNetURI;
    }

}
