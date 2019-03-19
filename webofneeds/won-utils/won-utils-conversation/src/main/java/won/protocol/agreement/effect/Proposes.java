package won.protocol.agreement.effect;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class Proposes extends MessageEffect {

    private Set<URI> proposes = new HashSet<>();
    private Set<URI> proposesToCancel = new HashSet<>();

    public Proposes(URI messageUri) {
        super(messageUri, MessageEffectType.PROPOSES);
    }

    void addProposes(URI uri) {
        proposes.add(uri);
    }

    void addProposesToCancel(URI uri) {
        proposesToCancel.add(uri);
    }

    public Set<URI> getProposes() {
        return proposes;
    }

    public Set<URI> getProposesToCancel() {
        return proposesToCancel;
    }

    public boolean hasCancellations() {
        return !this.proposesToCancel.isEmpty();
    }

    public boolean hasClauses() {
        return !this.proposes.isEmpty();
    }

    public ProposalType getProposalType() {
        if (hasClauses()) {
            if (hasCancellations()) {
                return ProposalType.PROPOSES_AND_CANCELS;
            }
            return ProposalType.PROPOSES;
        } else if ((hasCancellations())) {
            return ProposalType.CANCELS;
        }
        return ProposalType.NONE;
    }

    @Override
    public String toString() {
        return "Proposes [proposes=" + proposes + ", proposesToCancel=" + proposesToCancel + "]";
    }

}
