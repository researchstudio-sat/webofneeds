package won.protocol.agreement.effect;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Accepts extends MessageEffect {
    private URI acceptedProposalUri;
    private Set<URI> cancelledAgreementUris = new HashSet<URI>();

    public Accepts(URI messageUri, URI acceptedMessageUri, Collection<URI> cancelledAgreementUris) {
        super(messageUri, MessageEffectType.ACCEPTS);
        this.acceptedProposalUri = acceptedMessageUri;
        this.cancelledAgreementUris.addAll(cancelledAgreementUris);
    }

    public URI getAcceptedMessageUri() {
        return this.acceptedProposalUri;
    }

    public Set<URI> getCancelledAgreementURIs() {
        return this.cancelledAgreementUris;
    }

    @Override
    public String toString() {
        return "Accepts [acceptedProposalUri=" + acceptedProposalUri + ", cancelledAgreementURIs="
                        + this.cancelledAgreementUris + "]";
    }
}
