package won.node.facet;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class ReviewFacet extends HardcodedFacetConfig {

    public ReviewFacet() {
        super(FacetType.ReviewFacet.getURI());
        this.derivationProperties.add(WON.REVIEWS);
    }
    
    @Override
    public boolean isConnectionAllowedToType(URI remoteFacetType) {
        return FacetType.ReviewFacet.getURI().equals(remoteFacetType);
    }
    
    @Override
    public boolean isAutoOpen(URI remoteFacetType) {
        return true;
    }
}
