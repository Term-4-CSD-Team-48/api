package com.term_4_csd__50_001.api;

import static com.term_4_csd__50_001.api.GlobalExceptionHandler.translateMongoException;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.mongodb.MongoWriteException;
import com.term_4_csd__50_001.api.collections.UserCollection;
import com.term_4_csd__50_001.api.exceptions.ConflictException;
import com.term_4_csd__50_001.api.models.User;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

        private static final String[] PUBLIC_URLS =
                        {"/auth/login", "/auth/register", "/auth/verify-email"};

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http.csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(PUBLIC_URLS).permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                                                .maximumSessions(10))
                                .formLogin(form -> form.loginProcessingUrl("/auth/login")
                                                .loginPage("/not-implemented")
                                                .successHandler((_, _, _) -> {
                                                }).failureHandler((_, response, exception) -> {
                                                        String errorMessage =
                                                                        "Authentication failed";
                                                        if (exception instanceof BadCredentialsException) {
                                                                errorMessage = "Invalid email or password";
                                                                response.setStatus(
                                                                                HttpServletResponse.SC_UNAUTHORIZED);
                                                        } else if (exception instanceof DisabledException) {
                                                                errorMessage = exception
                                                                                .getMessage();
                                                                response.setStatus(
                                                                                HttpServletResponse.SC_FORBIDDEN);
                                                        } else if (exception instanceof LockedException) {
                                                                errorMessage = "User account is locked";
                                                                response.setStatus(
                                                                                HttpServletResponse.SC_FORBIDDEN);
                                                        }
                                                        response.getWriter().write(errorMessage);
                                                        response.getWriter().flush();
                                                }).usernameParameter("email"))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((_, res, ex) -> {
                                                        System.out.println(ex.getMessage());
                                                        res.setContentType("application/json");
                                                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        res.getWriter().write(
                                                                        "{\"error\": \"Unauthorized access\"}");
                                                        res.getWriter().flush();
                                                }));
                return http.build();

        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CustomAuthenticationManager customAuthenticationManager(HttpSecurity http,
                        PasswordEncoder passwordEncoder, UserCollection userCollection) {
                try {
                        userCollection.insertOne(User.builder(passwordEncoder)
                                        .email("yufeng_xue@mymail.sutd.edu.sg").username("yufeng")
                                        .password("password")
                                        .authorities(Collections.singletonList(
                                                        new GrantedAuthorityWrapper("ROLE_USER")))
                                        .emailVerified(true).enabled(true).build());
                } catch (MongoWriteException e) {
                        if (!(translateMongoException(e) instanceof ConflictException)) {
                                throw e;
                        }
                }
                return new CustomAuthenticationManager(userCollection, passwordEncoder);
        }

}
