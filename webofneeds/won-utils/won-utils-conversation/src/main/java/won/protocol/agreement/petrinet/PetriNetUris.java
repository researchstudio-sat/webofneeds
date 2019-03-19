package won.protocol.agreement.petrinet;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class PetriNetUris {

    // The URI chosen by one of the participants for this petri net
    private URI processURI;

    private Set<URI> enabledTransitions = new HashSet<>();

    private Set<URI> markedPlaces = new HashSet<>();

    public URI getProcessURI() {
        return processURI;
    }

    public void setPetriNetURI(URI processURI) {
        this.processURI = processURI;
    }

    public Set<URI> getEnabledTransitions() {
        return enabledTransitions;
    }

    public void setEnabledTransitions(Set<URI> enabledTransitions) {
        this.enabledTransitions = enabledTransitions;
    }

    public Set<URI> getMarkedPlaces() {
        return markedPlaces;
    }

    public void setMarkedPlaces(Set<URI> markedPlaces) {
        this.markedPlaces = markedPlaces;
    }

}
