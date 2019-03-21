package won.node.facet;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

import java.net.URI;

public class HoldableFacetConfig extends HardcodedFacetConfig {

  public HoldableFacetConfig() {
    super(FacetType.HoldableFacet.getURI());
    this.derivationProperties.add(WON.HELD_BY);
  }

  @Override
  public boolean isConnectionAllowedToType(URI remoteFacetType) {
    return FacetType.HolderFacet.getURI().equals(remoteFacetType);
  }

  @Override
  public boolean isAutoOpen(URI remoteFacetType) {
    return false;
  }
}
