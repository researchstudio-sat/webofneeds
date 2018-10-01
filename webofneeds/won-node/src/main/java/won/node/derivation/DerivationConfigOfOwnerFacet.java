package won.node.derivation;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class DerivationConfigOfOwnerFacet extends HardcodedFacetDerivationConfig {
    
    public DerivationConfigOfOwnerFacet() {
        this.derivationProperties.add(WON.CONNECTED_WITH);
    }
    
    @Override
    public URI getFacetType() {
        return FacetType.OwnerFacet.getURI();
    }
    
}
