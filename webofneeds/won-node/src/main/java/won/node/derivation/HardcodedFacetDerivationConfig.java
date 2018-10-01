package won.node.derivation;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

/**
 * Subclasses are expected to hardcode their config in their constructor.
 */
public abstract class HardcodedFacetDerivationConfig implements FacetDerivationConfig {
    
    protected Set<Property> derivationProperties = new HashSet<>();
    
    public HardcodedFacetDerivationConfig() {
        super();
    }

    @Override
    final public Set<Property> getDerivationProperties() {
        return derivationProperties;
    }
}
