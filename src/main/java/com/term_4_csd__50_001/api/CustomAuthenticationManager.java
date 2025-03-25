package com.term_4_csd__50_001.api;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.term_4_csd__50_001.api.collections.UserCollection;
import com.term_4_csd__50_001.api.models.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        String email = (String) authentication.getPrincipal();
        String rawPassword = (String) authentication.getCredentials();
        log.info(String.format("Manager authenticating with email (Principal): %s\n(Credentials)",
                email));
        User user = userCollection.findOne(User.builder().email(email).build());

        if (user == null)
            throw new BadCredentialsException("User not found");

        if (!user.isEnabled())
            throw new DisabledException("User disabled");

        if (!user.isEmailVerified())
            throw new BadCredentialsException("Email not verified");

        if (!passwordEncoder.matches(rawPassword, user.getPassword()))
            throw new BadCredentialsException("Invalid password");

        return CustomAuthenticationToken.authenticated(email, user.getPassword(),
                user.getAuthorities());
    }

}
