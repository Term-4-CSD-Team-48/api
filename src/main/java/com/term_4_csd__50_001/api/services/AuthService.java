package com.term_4_csd__50_001.api.services;

import java.util.Collections;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.term_4_csd__50_001.api.GrantedAuthorityWrapper;
import com.term_4_csd__50_001.api.collections.UpdateBuilder;
import com.term_4_csd__50_001.api.collections.UserCollection;
import com.term_4_csd__50_001.api.exceptions.BadRequestException;
import com.term_4_csd__50_001.api.models.User;

@Service
public class AuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserCollection userCollection;

    public void register(String email, String username, String password) {
        register(email, username, password, false);
    }

    public void register(String email, String username, String password, boolean verified) {
        User.Builder builder =
                new User.Builder(passwordEncoder).email(email).username(username).password(password)
                        .authorities(
                                Collections.singletonList(new GrantedAuthorityWrapper("ROLE_USER")))
                        .enabled(true);
        if (verified) {
            builder = builder.emailVerified(true);
        } else {
            builder = builder.emailVerificationToken(UUID.randomUUID().toString());
        }
        User user = builder.build();
        userCollection.insertOne(user);
    }

    public void unregister(String email, String rawPassword) {
        User findOne = User.builder().email(email).build();
        User user = userCollection.findOne(findOne);
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            userCollection.deleteOne(user);
        } else {
            throw new BadRequestException(
                    "Password provided does not match with user that must be deleted");
        }
    }

    public void verifyEmail(String token) {
        User user = User.builder().emailVerificationToken(token).build();
        User set = User.builder().emailVerified(true).build();
        User unset = User.builder().emailVerificationToken("").build();
        UpdateBuilder<User> update = new UpdateBuilder<User>().set(set).unset(unset);
        userCollection.updateOne(user, update);
    }

}
