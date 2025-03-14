package com.term_4_csd__50_001.api;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.term_4_csd__50_001.api.collections.UserCollection;
import com.term_4_csd__50_001.api.models.User;

public class CustomAuthenticationManager implements AuthenticationManager {

    private final UserCollection userCollection;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationManager(UserCollection userCollection,
            PasswordEncoder passwordEncoder) {
        this.userCollection = userCollection;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CustomAuthenticationToken authenticate(Authentication authentication)
            throws AuthenticationException {
        System.out.println("Manager authenticating");
        String email = (String) authentication.getPrincipal();
        String rawPassword = (String) authentication.getCredentials();
        System.out.println("(Principal) Email: " + email);
        System.out.println(
                "(Credentials) Password: " + (rawPassword.length() > 0 ? "[RECEIVED]" : "[EMPTY]"));

        User user = userCollection.findOne(User.builder().email(email).build());

        if (user == null) {
            throw new BadCredentialsException("User not found");
        }

        // if (!user.isAccountNonExpired()) {
        // throw new DisabledException("User expired");
        // }

        // if (!user.isCredentialsNonExpired()) {
        // throw new DisabledException("User credentials expired");
        // }

        if (!user.isEnabled()) {
            throw new DisabledException("User disabled");
        }

        if (!user.isEmailVerified()) {
            throw new DisabledException("Email not verified");
        }

        // if (!user.isAccountNonLocked()) {
        // throw new LockedException("User locked");
        // }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        System.out.println(
                "Hashed password: " + (user.getPassword().length() > 0 ? "[RECEIVED]" : "[EMPTY]"));

        return CustomAuthenticationToken.authenticated(email, user.getPassword(),
                user.getAuthorities());
    }

}
