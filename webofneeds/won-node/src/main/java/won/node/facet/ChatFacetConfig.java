package won.node.facet;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class ChatFacetConfig extends HardcodedFacetDerivationConfig {
    
    public ChatFacetConfig() {
        this.derivationProperties.add(WON.CONNECTED_WITH);
    }
    
    @Override
    public URI getFacetType() {
        return FacetType.ChatFacet.getURI();
    }
    
    /**
     * For now, we treat the chat facet as the default facet that
     * is itself allowed to connect to all.
     */
    @Override
    public boolean isConnectionAllowedToType(URI remoteFacetType) {
        return true;
    }
}
