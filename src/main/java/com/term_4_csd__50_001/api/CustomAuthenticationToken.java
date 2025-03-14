package com.term_4_csd__50_001.api;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final String email;
    private String rawPassword;

    public CustomAuthenticationToken(String email, String rawPassword) {
        super(null);
        this.email = email;
        this.rawPassword = rawPassword;
        setAuthenticated(false);
    }

    public CustomAuthenticationToken(String email, String rawPassword,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.email = email;
        this.rawPassword = rawPassword;
        super.setAuthenticated(true);
    }

    public static CustomAuthenticationToken unauthenticated(String email, String rawPassword) {
        return new CustomAuthenticationToken(email, rawPassword);
    }

    public static CustomAuthenticationToken authenticated(String email, String rawPassword,
            Collection<? extends GrantedAuthority> authorities) {
        return new CustomAuthenticationToken(email, rawPassword, authorities);
    }

    @Override
    public String getCredentials() {
        return this.rawPassword;
    }

    @Override
    public String getPrincipal() {
        return this.email;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        Assert.isTrue(!isAuthenticated,
                "Cannot set this token to trusted - make another token instead");
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.rawPassword = null;
    }

}
