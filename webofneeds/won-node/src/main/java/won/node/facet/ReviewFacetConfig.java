package won.node.facet;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

import java.net.URI;

public class ReviewFacetConfig extends HardcodedFacetConfig {

  public ReviewFacetConfig() {
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
