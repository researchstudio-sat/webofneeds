package won.protocol.exception;

import java.net.URI;

public class IncompatibleFacetTypesException extends WonProtocolException {
    public IncompatibleFacetTypesException(URI localFacet, URI localFacetType, URI remoteFacet, URI remoteFacetType) {
        super("Incompatible facets! Local facet: " + localFacet + " (type: " + localFacetType + "), remote facet: "
                        + remoteFacet + " (type: " + remoteFacetType + ")");
    }
}
