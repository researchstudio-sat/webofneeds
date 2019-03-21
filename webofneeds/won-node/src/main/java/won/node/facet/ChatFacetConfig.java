package won.node.facet;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

import java.net.URI;

public class ChatFacetConfig extends HardcodedFacetConfig {

  public ChatFacetConfig() {
    super(FacetType.ChatFacet.getURI());
    this.derivationProperties.add(WON.CONNECTED_WITH);
  }

  /**
   * For now, we treat the chat facet as the default facet that is itself allowed
   * to connect to all.
   */
  @Override
  public boolean isConnectionAllowedToType(URI remoteFacetType) {
    return true;
  }

  @Override
  public boolean isAutoOpen(URI remoteFacetType) {
    return false;
  }
}
