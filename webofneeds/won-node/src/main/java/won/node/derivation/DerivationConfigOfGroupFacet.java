package won.node.derivation;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class DerivationConfigOfGroupFacet extends HardcodedFacetDerivationConfig {

    @Override
    public URI getFacetType() {
        return FacetType.GroupFacet.getURI();
    }
    
    public DerivationConfigOfGroupFacet() {
        this.derivationProperties.add(WON.HAS_GROUP_MEMBER);
    }
}
