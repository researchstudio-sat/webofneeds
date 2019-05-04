package won.protocol.agreement;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ProposalUris {
    private URI uri;
    private URI proposingAtomUri;
    private Set<URI> proposes = new HashSet<>();
    private Set<URI> proposesToCancel = new HashSet<>();

    public ProposalUris(URI uri, URI atomUri) {
        this.uri = uri;
        this.proposingAtomUri = atomUri;
    }

    public URI getUri() {
        return uri;
    }

    public URI getProposingAtomUri() {
        return proposingAtomUri;
    }

    public Set<URI> getProposes() {
        return proposes;
    }

    public Set<URI> getProposesToCancel() {
        return proposesToCancel;
    }

    public void addProposes(URI uri) {
        this.proposes.add(uri);
    }

    public void addProposes(Collection<URI> uris) {
        this.proposes.addAll(uris);
    }

    public void addProposesToCancel(URI uri) {
        this.proposesToCancel.add(uri);
    }

    public void addProposesToCancel(Collection<URI> uris) {
        this.proposesToCancel.addAll(uris);
    }
}