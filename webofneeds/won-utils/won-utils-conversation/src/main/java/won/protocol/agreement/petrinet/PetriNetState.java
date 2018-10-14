package won.protocol.agreement.petrinet;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;

import uk.ac.imperial.pipe.animation.PetriNetAnimator;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;

public class PetriNetState {

    // the petri net
    private PetriNet petriNet;

    // the animator (allowing us to change the pn's state)
    private PetriNetAnimator petriNetAnimator;

    // The URI chosen by one of the participants for this petri net
    private URI petrinetURI;

    // indicates that there are conflicting interpretations about the state of the
    // petri net
    private boolean inConflict = false;

    // indicates the participants in whose point of view the petri net is in the
    // state we are describing. If there are no conflicting interpretations,
    // this contains all participants.
    private Set<URI> participants;

    // holds the xml defining the petri net
    private String definition;

    // derived data for the state of the petrinet
    private Dataset derivedData;

    private Set<URI> markedPlaces = new HashSet<>();

    private Set<URI> enabledTransitions = new HashSet<>();

    private Set<URI> places = new HashSet<>();

    private Set<URI> transitions = new HashSet<>();

    public PetriNetState(URI petrinetURI, PetriNet petriNet) {
        super();
        this.petrinetURI = petrinetURI;
        this.petriNet = petriNet;
        this.petriNetAnimator = new PetriNetAnimator(petriNet);
    }

    /**
     * Returns the set of URIs associatied with all marked places. Nothing is
     * reported for places that are not associatied with a URI.
     */
    public Set<URI> getMarkedPlaces() {
        return petriNet.getPlaces().stream().filter(place -> place.getNumberOfTokensStored() > 0)
                .map(place -> URI.create(place.getId())).collect(Collectors.toSet());
    }

    /**
     * Returns the set of URIs associatied with all enabled transitions. Nothing is
     * reported for transitions that are not associatied with a URI.
     */
    public Set<URI> getEnabledTransitions() {
        return petriNetAnimator.getEnabledTransitions().stream().map(t -> URI.create(t.getId()))
                .collect(Collectors.toSet());
    }
    
    public void fireForEvent(URI eventUri) {
        petriNetAnimator.getEnabledTransitions()
             .stream()
             .filter(t -> eventUri.toString().equals(t.getId()))
             .forEach(petriNetAnimator::fireTransition);
    }

    public Set<URI> getPlaces() {
        return petriNet.getPlaces().stream().map(p -> URI.create(p.getId())).collect(Collectors.toSet());
    }

    public Set<URI> getTransitions() {
        return petriNet.getTransitions().stream().map(p -> URI.create(p.getId())).collect(Collectors.toSet());
    }

    /**
     * Returns all data that can be derived from the current state of the petrinet.
     * This dataset may change whenever the petrinet state changes.
     */
    public Dataset getDerivedData() {
        return derivedData;
    }

}
