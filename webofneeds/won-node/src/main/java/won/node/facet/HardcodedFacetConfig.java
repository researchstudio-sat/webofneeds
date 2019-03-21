package won.node.facet;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Property;

/**
 * Subclasses are expected to hardcode their config in their constructor.
 */
public abstract class HardcodedFacetConfig implements FacetConfig {
    
    private URI facetType;
    protected Set<Property> derivationProperties = new HashSet<>();
    
    public HardcodedFacetConfig(URI facetType) {
        this.facetType = facetType;
    }
    
    @Override
    public final URI getFacetType() {
        return facetType;
    }

    @Override
    final public Set<Property> getDerivationProperties() {
        return derivationProperties;
    }
}
