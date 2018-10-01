package won.node.derivation;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class DerivationConfigOfOwnableFacet extends HardcodedFacetDerivationConfig {
    
    public DerivationConfigOfOwnableFacet() {
        this.derivationProperties.add(WON.OWNED_BY);
    }
    
    @Override
    public URI getFacetType() {
        return FacetType.GroupFacet.getURI();
    }
}
