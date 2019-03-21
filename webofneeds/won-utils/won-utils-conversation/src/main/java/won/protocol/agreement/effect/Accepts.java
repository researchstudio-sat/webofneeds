package won.protocol.agreement.effect;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Accepts extends MessageEffect {

  private URI acceptedProposalUri;
  private Set<URI> cancelledAgreementURIs = new HashSet<URI>();

  public Accepts(URI messageUri, URI acceptedMessageUri, Collection<URI> cancelledAgreementUris) {
    super(messageUri, MessageEffectType.ACCEPTS);
    this.acceptedProposalUri = acceptedMessageUri;
    this.cancelledAgreementURIs.addAll(cancelledAgreementURIs);
  }

  public URI getAcceptedMessageUri() {
    return acceptedProposalUri;
  }

  public Set<URI> getCancelledAgreementURIs() {
    return cancelledAgreementURIs;
  }

  @Override
  public String toString() {
    return "Accepts [acceptedProposalUri=" + acceptedProposalUri + ", cancelledAgreementURIs=" + cancelledAgreementURIs
        + "]";
  }

}
