package won.node.facet;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class HoldableFacetConfig extends HardcodedFacetDerivationConfig {
    
    public HoldableFacetConfig() {
        this.derivationProperties.add(WON.HELD_BY);
    }
    
    @Override
    public URI getFacetType() {
        return FacetType.HoldableFacet.getURI();
    }
    
    @Override
    public boolean isConnectionAllowedToType(URI remoteFacetType) {
        return FacetType.HolderFacet.getURI().equals(remoteFacetType);
    }
}
