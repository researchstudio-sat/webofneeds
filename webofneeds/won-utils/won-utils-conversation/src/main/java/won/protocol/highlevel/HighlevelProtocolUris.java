package won.protocol.highlevel;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class HighlevelProtocolUris {
	private Set<URI> retracted = new HashSet<URI>();
	private Set<URI> rejected = new HashSet<URI>();
	private Set<URI> agreements = new HashSet<URI>();
	private Set<URI> pendingProposals = new HashSet<URI>();
	private Set<URI> cancelledAgreements = new HashSet<URI>();
	private Set<URI> cancellationPendingAgreements = new HashSet<URI>();
	private Set<URI> pendingCancellationProposals = new HashSet<URI>();
	private Set<URI> acceptedCancellationProposals = new HashSet<URI>();
	
	public HighlevelProtocolUris() {
	
	}
	
	public void addRetractedMessageUris(Collection<URI> uris) {
		this.retracted.addAll(uris);
	}
	
	public void addRejectedMessageUris(Collection<URI> uris) {
		this.rejected.addAll(uris);
	}
	
	public void addAgreementUris(Collection<URI> uris) {
		this.agreements.addAll(uris);
	}
	
	public void addPendingProposalUris(Collection<URI> uris) {
		this.pendingProposals.addAll(uris);
	}
	
	public void addCancelledAgreementUris(Collection<URI> uris) {
		this.cancelledAgreements.addAll(uris);
	}
	
	public void addCancellationPendingAgreementUris(Collection<URI> uris) {
		this.cancellationPendingAgreements.addAll(uris);
	}
	
	public void addPendingCancellationProposalUris(Collection<URI> uris) {
		this.pendingCancellationProposals.addAll(uris);
	}
	
	public void addAcceptedCancellationProposalUris(Collection<URI> uris) {
		this.acceptedCancellationProposals.addAll(uris);
	}

	public Set<URI> getRetractedMessageUris() {
		return retracted;
	}

	public Set<URI> getRejectedMessageUris() {
		return rejected;
	}

	public Set<URI> getAgreementUris() {
		return agreements;
	}

	public Set<URI> getPendingProposalUris() {
		return pendingProposals;
	}

	public Set<URI> getCancelledAgreementUris() {
		return cancelledAgreements;
	}

	public Set<URI> getCancellationPendingAgreementUris() {
		return cancellationPendingAgreements;
	}

	public Set<URI> getPendingCancellationProposalUris() {
		return pendingCancellationProposals;
	}

	public Set<URI> getAcceptedCancellationProposalUris() {
		return acceptedCancellationProposals;
	}

	@Override
	public String toString() {
		return "HighlevelProtocolUris [retracted=" + retracted + ", rejected=" + rejected + ", agreements=" + agreements
				+ ", pendingProposals=" + pendingProposals + ", cancelledAgreements=" + cancelledAgreements
				+ ", cancellationPendingAgreements=" + cancellationPendingAgreements + ", pendingCancellationProposals="
				+ pendingCancellationProposals + ", acceptedCancellationProposals=" + acceptedCancellationProposals
				+ "]";
	}

	
	
}
