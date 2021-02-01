package won.node.springsecurity.acl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import won.auth.AuthUtils;
import won.auth.model.AuthToken;

import java.util.ArrayDeque;
import java.util.Collection;

public class WonAclTokenAuthentication implements Authentication {
    private String encodedToken;
    private AuthToken authToken;

    public WonAclTokenAuthentication(String encodedToken, AuthToken authToken) {
        this.encodedToken = encodedToken;
        this.authToken = authToken;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return encodedToken;
    }

    @Override
    public Object getDetails() {
        return authToken;
    }

    @Override
    public Object getPrincipal() {
        return authToken != null ? authToken.getTokenSub() : false;
    }

    @Override
    public boolean isAuthenticated() {
        return authToken != null;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean equals(Object another) {
        return this.authToken
                        .deepEquals((AuthToken) ((WonAclTokenAuthentication) another).getDetails(), new ArrayDeque());
    }

    @Override
    public String toString() {
        return authToken != null ? AuthUtils.toRdfString(authToken) : "[AuthToken is null]";
    }

    @Override
    public int hashCode() {
        return encodedToken != null ? encodedToken.hashCode() : "null".hashCode();
    }

    @Override
    public String getName() {
        return null;
    }
}
