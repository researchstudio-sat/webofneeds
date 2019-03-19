package won.node.facet;

import java.net.URI;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class GroupFacetConfig extends HardcodedFacetConfig {

    public GroupFacetConfig() {
        super(FacetType.GroupFacet.getURI());
        this.derivationProperties.add(WON.HAS_GROUP_MEMBER);
    }

    @Override
    public boolean isConnectionAllowedToType(URI remoteFacetType) {
        return FacetType.ChatFacet.getURI().equals(remoteFacetType);
    }

    @Override
    public boolean isAutoOpen(URI remoteFacetType) {
        return false;
    }
}
