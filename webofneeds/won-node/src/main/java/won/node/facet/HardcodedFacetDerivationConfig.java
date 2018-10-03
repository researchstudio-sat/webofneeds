package won.node.facet;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

/**
 * Subclasses are expected to hardcode their config in their constructor.
 */
public abstract class HardcodedFacetDerivationConfig implements FacetConfig {
    
    protected Set<Property> derivationProperties = new HashSet<>();
    
    public HardcodedFacetDerivationConfig() {
        super();
    }

    @Override
    final public Set<Property> getDerivationProperties() {
        return derivationProperties;
    }
}
