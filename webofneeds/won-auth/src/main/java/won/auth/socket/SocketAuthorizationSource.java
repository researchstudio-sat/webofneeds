package won.auth.socket;

import java.net.URI;
import java.util.Optional;

public interface SocketAuthorizationSource {
    Optional<SocketAuthorizations> getSocketAuthorizations(URI socketDefinitionURI);
}
