package won.node.facet;

import java.net.URI;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class HolderFacetConfig extends HardcodedFacetDerivationConfig {

   
    public HolderFacetConfig() {
        this.derivationProperties.add(WON.HOLDS);
    }
    
    @Override
    public URI getFacetType() {
        return FacetType.HolderFacet.getURI();
    }
    
    @Override
    public boolean isConnectionAllowedToType(URI remoteFacetType) {
        return FacetType.HoldableFacet.getURI().equals(remoteFacetType);
    }

}
