package won.node.derivation;

import java.net.URI;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

import won.protocol.model.FacetType;
import won.protocol.vocabulary.WON;

public class DerivationConfigOfHolderFacet extends HardcodedFacetDerivationConfig {

   
    public DerivationConfigOfHolderFacet() {
        this.derivationProperties.add(WON.HOLDS);
    }
    
    @Override
    public URI getFacetType() {
        return FacetType.HolderFacet.getURI();
    }

}
