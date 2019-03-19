package won.owner.service.impl;

import java.security.KeyStore;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import won.owner.model.User;

public class KeystoreEnabledUserDetails implements UserDetails {

    private User delegate;
    private KeyStore keyStore;
    private String password;

    public KeystoreEnabledUserDetails(User delegate, KeyStore keyStore, String password) {
        super();
        this.delegate = delegate;
        this.keyStore = keyStore;
        this.password = password;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public String getKeystorePassword() {
        return password;
    }

    public User getUser() {
        return delegate;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    public String getPassword() {
        return delegate.getPassword();
    }

    public String getUsername() {
        return delegate.getUsername();
    }

    public boolean isAccountNonExpired() {
        return delegate.isAccountNonExpired();
    }

    public boolean isAccountNonLocked() {
        return delegate.isAccountNonLocked();
    }

    public boolean isCredentialsNonExpired() {
        return delegate.isCredentialsNonExpired();
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

}
