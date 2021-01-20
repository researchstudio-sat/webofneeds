package won.node.springsecurity.userdetails;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class AnonymousUserDetails extends AbstractUserDetails {
    public AnonymousUserDetails(
                    Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }
}
