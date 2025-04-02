package com.term_4_csd__50_001.api.services;

import java.util.Collections;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.term_4_csd__50_001.api.GrantedAuthorityWrapper;
import com.term_4_csd__50_001.api.collections.UpdateBuilder;
import com.term_4_csd__50_001.api.collections.UserCollection;
import com.term_4_csd__50_001.api.exceptions.UnauthorizedRequestException;
import com.term_4_csd__50_001.api.models.User;
import com.term_4_csd__50_001.api.services.MailService.MailBuilder;

@Service
public class AuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserCollection userCollection;
    @Autowired
    private MailService mailService;
    @Autowired
    private IPService ipService;

    public void register(String email, String username, String password) {
        register(email, username, password, false);
    }

    public void register(String email, String username, String password, boolean verified) {
        User.Builder builder =
                new User.Builder(passwordEncoder).email(email).username(username).password(password)
                        .authorities(
                                Collections.singletonList(new GrantedAuthorityWrapper("ROLE_USER")))
                        .enabled(true);
        String emailVerificationToken = UUID.randomUUID().toString();
        if (verified) {
            builder = builder.emailVerified(true);
        } else {
            builder = builder.emailVerificationToken(emailVerificationToken);
        }
        User user = builder.build();
        userCollection.insertOne(user);
        sendVerificationEmail(emailVerificationToken, email);
    }

    private void sendVerificationEmail(String token, String email) {
        String publicIP = ipService.getSelfPublicIP();
        MailBuilder mailBuilder = mailService.mailBuilder()
                .text(String.format("http://%s:8080/auth/verify-email?email-verification-token=%s",
                        publicIP, token))
                .recipients(email).subject("Email verification for ParcelEye");
        mailService.sendMail(mailBuilder);
    }

    public void unregister(String email, String rawPassword) {
        User findOne = User.builder().email(email).build();
        User user = userCollection.findOne(findOne);
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            userCollection.deleteOne(user);
        } else {
            throw new UnauthorizedRequestException(
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
