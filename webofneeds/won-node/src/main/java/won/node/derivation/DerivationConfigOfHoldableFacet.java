package won.node.derivation;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class DerivationConfigOfHoldableFacet extends HardcodedFacetDerivationConfig {
    
    public DerivationConfigOfHoldableFacet() {
        this.derivationProperties.add(WON.HELD_BY);
    }
    
    @Override
    public URI getFacetType() {
        return FacetType.HoldableFacet.getURI();
    }
}
