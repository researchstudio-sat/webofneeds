package won.auth.socket;

import won.auth.model.Authorization;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class SocketAuthorizations {
    private URI socketUri;
    private Set<Authorization> localAuths;
    private Set<Authorization> targetAuths;

    public SocketAuthorizations(URI socketUri, Set<Authorization> localAuths,
                    Set<Authorization> requests) {
        this.socketUri = socketUri;
        this.localAuths = Collections.unmodifiableSet(localAuths != null ? localAuths : Collections.emptySet());
        this.targetAuths = Collections.unmodifiableSet(requests != null ? localAuths : Collections.emptySet());
    }

    public Set<Authorization> getLocalAuths() {
        return localAuths;
    }

    public Set<Authorization> getTargetAuths() {
        return targetAuths;
    }

    public URI getSocketUri() {
        return socketUri;
    }
}
