package won.auth.socket;

import java.net.URI;

public interface SocketAuthorizationSource {
    SocketAuthorizations getSocketAuthorizations(URI socketDefinitionURI);
}
