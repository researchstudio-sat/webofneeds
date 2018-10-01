package won.node.derivation;

import java.net.URI;
import java.util.Set;

import org.apache.jena.rdf.model.Property;


public interface FacetDerivationConfig {
    /*
     * Returns the type that this confi is applicable for 
     */
    public URI getFacetType(); 
    
    /**
     * Returns the set of derivation properties to be used for this type.
     */
    public Set<Property> getDerivationProperties();
}
