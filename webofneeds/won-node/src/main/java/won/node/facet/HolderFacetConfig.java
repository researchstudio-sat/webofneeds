package won.node.facet;

import java.net.URI;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class HolderFacetConfig extends HardcodedFacetConfig {

   
    public HolderFacetConfig() {
        super(FacetType.HolderFacet.getURI());
        this.derivationProperties.add(WON.HOLDS);
    }
    
    @Override
    public boolean isConnectionAllowedToType(URI remoteFacetType) {
        return FacetType.HoldableFacet.getURI().equals(remoteFacetType);
    }

}
