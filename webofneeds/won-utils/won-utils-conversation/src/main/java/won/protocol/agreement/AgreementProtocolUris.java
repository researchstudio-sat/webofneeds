package won.protocol.agreement;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AgreementProtocolUris {
    private Set<URI> retractedUris = new HashSet<URI>();
    private Set<URI> rejectedUris = new HashSet<URI>();
    private Set<URI> claimedUris = new HashSet<URI>();
    private Set<URI> proposedUris = new HashSet<URI>();
    private Set<URI> proposedToCancelUris = new HashSet<URI>();
    private Set<URI> agreementUris = new HashSet<URI>();
    private Set<URI> agreedMessageUris = new HashSet<URI>();
    private Set<URI> pendingProposalUris = new HashSet<URI>();
    private Set<URI> cancelledAgreementUris = new HashSet<URI>();
    private Set<URI> cancellationPendingAgreementUris = new HashSet<URI>();
    private Set<URI> pendingCancellationProposalUris = new HashSet<URI>();
    private Set<URI> acceptedCancellationProposalUris = new HashSet<URI>();
    private Set<ProposalUris> pendingProposals = new HashSet<ProposalUris>();

    public AgreementProtocolUris() {
    }

    public void addRetractedMessageUris(Collection<URI> uris) {
        this.retractedUris.addAll(uris);
    }

    public void addRejectedMessageUris(Collection<URI> uris) {
        this.rejectedUris.addAll(uris);
    }

    public void addProposedMessageUris(Collection<URI> uris) {
        this.proposedUris.addAll(uris);
    }

    public void addProposedMessageUri(URI uri) {
        this.proposedUris.add(uri);
    }

    public void addProposedToCancelUris(Set<URI> uris) {
        this.proposedToCancelUris.addAll(uris);
    }

    public void addProposedToCancelUri(URI uri) {
        this.proposedToCancelUris.add(uri);
    }

    public void addClaimedMessageUris(Collection<URI> uris) {
        this.claimedUris.addAll(uris);
    }

    public void addClaimedMessageUri(URI uri) {
        this.claimedUris.add(uri);
    }

    public void addAgreementUris(Collection<URI> uris) {
        this.agreementUris.addAll(uris);
    }

    public void addAgreedMessageUris(Set<URI> agreedMessageUris) {
        this.agreedMessageUris.addAll(agreedMessageUris);
    }

    public void addAgreedMessageUri(URI agreedMessageUri) {
        this.agreedMessageUris.add(agreedMessageUri);
    }

    public void addPendingProposalUris(Collection<URI> uris) {
        this.pendingProposalUris.addAll(uris);
    }

    public void addPendingProposalUri(URI uri) {
        this.pendingProposalUris.add(uri);
    }

    public void addPendingProposal(ProposalUris proposal) {
        this.pendingProposals.add(proposal);
    }

    public void addCancelledAgreementUris(Collection<URI> uris) {
        this.cancelledAgreementUris.addAll(uris);
    }

    public void addCancellationPendingAgreementUris(Collection<URI> uris) {
        this.cancellationPendingAgreementUris.addAll(uris);
    }

    public void addPendingCancellationProposalUris(Collection<URI> uris) {
        this.pendingCancellationProposalUris.addAll(uris);
    }

    public void addPendingCancellationProposalUri(URI uri) {
        this.pendingCancellationProposalUris.add(uri);
    }

    public void addAcceptedCancellationProposalUris(Collection<URI> uris) {
        this.acceptedCancellationProposalUris.addAll(uris);
    }

    public Set<URI> getRetractedMessageUris() {
        return retractedUris;
    }

    public Set<URI> getRejectedMessageUris() {
        return rejectedUris;
    }

    public Set<URI> getProposedMessageUris() {
        return proposedUris;
    }

    public Set<URI> getProposedToCancelUris() {
        return proposedToCancelUris;
    }

    public Set<URI> getClaimedMessageUris() {
        return claimedUris;
    }

    public Set<URI> getAgreementUris() {
        return agreementUris;
    }

    public Set<URI> getAgreedMessageUris() {
        return agreedMessageUris;
    }

    public Set<URI> getPendingProposalUris() {
        return pendingProposalUris;
    }

    public Set<URI> getCancelledAgreementUris() {
        return cancelledAgreementUris;
    }

    public Set<URI> getCancellationPendingAgreementUris() {
        return cancellationPendingAgreementUris;
    }

    public Set<URI> getPendingCancellationProposalUris() {
        return pendingCancellationProposalUris;
    }

    public Set<URI> getAcceptedCancellationProposalUris() {
        return acceptedCancellationProposalUris;
    }

    public Set<ProposalUris> getPendingProposals() {
        return pendingProposals;
    }

    @Override
    public String toString() {
        return "AgreementProtocolUris [retracted=" + retractedUris + ", rejected=" + rejectedUris + ", claimed="
                        + claimedUris + ", agreements=" + agreementUris + ", agreedMessageUris=" + agreedMessageUris
                        + ", pendingProposals=" + pendingProposalUris
                        + ", cancelledAgreements=" + cancelledAgreementUris + ", cancellationPendingAgreements="
                        + cancellationPendingAgreementUris + ", pendingCancellationProposals="
                        + pendingCancellationProposalUris + ", acceptedCancellationProposals="
                        + acceptedCancellationProposalUris + ", proposedToCancelUris=" + proposedToCancelUris + "]";
    }
}
