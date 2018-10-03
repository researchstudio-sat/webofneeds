package won.node.facet;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class RateFacet extends HardcodedFacetConfig {

    public RateFacet() {
        super(FacetType.RateFacet.getURI());
        this.derivationProperties.add(WON.RATES);
    }
    
    @Override
    public boolean isConnectionAllowedToType(URI remoteFacetType) {
        return FacetType.RateFacet.getURI().equals(remoteFacetType);
    }
}
