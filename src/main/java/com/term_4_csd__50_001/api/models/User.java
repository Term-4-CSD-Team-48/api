package com.term_4_csd__50_001.api.models;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.term_4_csd__50_001.api.GrantedAuthorityWrapper;
import com.term_4_csd__50_001.api.exceptions.BadRequestException;

/**
 * Password is always stored as a hash All Boolean fields are treated as true by their getters if
 * they are set to null
 */
public class User {

    public List<GrantedAuthorityWrapper> authorities;
    public String email;
    public String emailVerificationToken;
    public Boolean emailVerified;
    public Boolean enabled;
    public String password;
    public String username;

    public User() {}

    private User(Builder builder) {
        this.authorities = builder.authorities;
        this.email = builder.email;
        this.emailVerificationToken = builder.emailVerificationToken;
        this.emailVerified = builder.emailVerified;
        this.enabled = builder.enabled;
        this.password = builder.password;
        this.username = builder.username;
    }

    public List<GrantedAuthorityWrapper> getAuthorities() {
        return authorities;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(PasswordEncoder passwordEncoder) {
        return new Builder(passwordEncoder);
    }

    public static class Builder {

        private PasswordEncoder passwordEncoder;
        private List<GrantedAuthorityWrapper> authorities;
        private String email;
        private String emailVerificationToken;
        private Boolean emailVerified;
        private Boolean enabled;
        private String password;
        private String username;
        public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern
                .compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

        /**
         * Not passing PasswordEncoder in the constructor will cause nullptr exception if password
         * method is called subsequently. This is to enforce all users to have their passwords
         * hashed if a password is to be set.
         */
        public Builder() {}

        public Builder(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }

        public Builder authorities(List<GrantedAuthorityWrapper> authorities) {
            this.authorities = authorities;
            return this;
        }

        public Builder email(String email) {
            if (!VALID_EMAIL_ADDRESS_REGEX.matcher(email).matches())
                throw new BadRequestException("Email is not valid!");
            this.email = email;
            return this;
        }

        public Builder emailVerificationToken(String emailVerificationToken) {
            this.emailVerificationToken = emailVerificationToken;
            return this;
        }

        public Builder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder password(String password) {
            this.password = passwordEncoder.encode(password);
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public User build() {
            return new User(this);
        }

    }

}
