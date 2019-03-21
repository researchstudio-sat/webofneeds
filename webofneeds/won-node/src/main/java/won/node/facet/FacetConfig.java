package won.node.facet;

import org.apache.jena.rdf.model.Property;

import java.net.URI;
import java.util.Set;

public interface FacetConfig {
  /*
   * Returns the type that this confi is applicable for
   */
  public URI getFacetType();

  /**
   * Returns the set of derivation properties to be used for this type.
   */
  public Set<Property> getDerivationProperties();

  /**
   * Indicates if a connection between this facet and the specified facet is
   * allowed.
   */
  public boolean isConnectionAllowedToType(URI remoteFacetType);

  /**
   * Indicates if a connection to the specified remote facet reacts to a connect
   * automatically with an open.
   */
  public boolean isAutoOpen(URI remoteFacetType);
}
