package com.term_4_csd__50_001.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.term_4_csd__50_001.api.models.User;

public class AuthControllerTest extends BaseTest {

        @Test
        public void userLifeCycleTest() throws Exception {
                String emailFieldName = "email";
                String usernameFieldName = "username";
                String passwordFieldName = "password";
                String email = "xyf.oco@gmail.com";
                String username = "username";
                String rawPassword = "password";

                // Register the above user
                webTestClient.post().uri(registerEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(emailFieldName + "=" + email + "&" + usernameFieldName
                                                + "=" + username + "&" + passwordFieldName + "="
                                                + rawPassword)
                                .exchange().expectStatus().isOk().expectBody().returnResult()
                                .getResponseHeaders();

                // Should throw up conflict error as we registering same user
                webTestClient.post().uri(registerEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(emailFieldName + "=" + email + "&" + usernameFieldName
                                                + "=" + username + "&" + passwordFieldName + "="
                                                + rawPassword)
                                .exchange().expectStatus().isEqualTo(409);

                // Should throw an error as we haven't verified email but login
                System.out.println(webTestClient.post().uri(loginEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(emailFieldName + "=" + email + "&" + passwordFieldName
                                                + "=" + rawPassword)
                                .exchange().expectStatus().isUnauthorized().expectBody(String.class)
                                .isEqualTo("Not").returnResult().getResponseBody());

                // Checking database directly to see if user is registered
                User findOne = User.builder().email(email).build();
                User user = userCollection.findOne(findOne);
                System.out.println("Found supposedly registered user with email " + user.email);
                if (user.emailVerificationToken == null)
                        throw new RuntimeException("No email verification token found");
                System.out.println("User has token " + user.emailVerificationToken);
                System.out.println("User email verified is " + user.emailVerified);

                // Verifying email
                final String emailVerificationToken = user.emailVerificationToken;
                webTestClient.get()
                                .uri(uri -> uri.path(verifyEmailEP)
                                                .queryParam(emailVerificationTokenURLParam,
                                                                emailVerificationToken)
                                                .build())
                                .exchange().expectStatus().isOk();

                // Checking database to see if verified
                user = userCollection.findOne(findOne);
                if (!user.emailVerified)
                        throw new RuntimeException("Failed to verify");
                if (!(user.emailVerificationToken == null))
                        System.out.println("User has token " + user.emailVerificationToken);
                System.out.println("User email verified is " + user.emailVerified);

                // Attempting to login with wrong emails and passwords
                webTestClient.post().uri(loginEP).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(emailFieldName + "=" + "wrongEmail" + "&"
                                                + passwordFieldName + "=" + rawPassword)
                                .exchange().expectStatus().isUnauthorized();
                webTestClient.post().uri(loginEP).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(emailFieldName + "=" + email + "&" + passwordFieldName
                                                + "=" + "wrongPassword")
                                .exchange().expectStatus().isUnauthorized();

                // Login with correct credentials
                HttpHeaders headers = webTestClient.post().uri(loginEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(emailFieldName + "=" + email + "&" + passwordFieldName
                                                + "=" + rawPassword)
                                .exchange().expectBody().returnResult().getResponseHeaders();

                System.out.println(String.valueOf(headers));
                String setCookieHeader = headers.getFirst(HttpHeaders.SET_COOKIE);
                String jSessionId = extractJSessionIdFromSetCookieHeader(setCookieHeader);
                System.out.println("Extracted JSESSIONID: " + jSessionId);

                // Access protected endpoint
                webTestClient.post().uri(authenticatedEP).cookie(jSessionIdCookieName, jSessionId)
                                .exchange().expectStatus().isOk();

                // Access protected endpoint with wrong jSessionId cookie
                webTestClient.post().uri(authenticatedEP)
                                .cookie(jSessionIdCookieName, "wrongJSessionId").exchange()
                                .expectStatus().isUnauthorized();

                // Logout with wrong jSessionId cookie
                webTestClient.post().uri(logoutEP).cookie(jSessionIdCookieName, "wrongJSessionId")
                                .exchange().expectStatus().isUnauthorized();

                // Logout
                webTestClient.post().uri(logoutEP).cookie(jSessionIdCookieName, jSessionId)
                                .exchange().expectStatus().isOk();

                // Access protected endpoint with logged out jSessionId cookie
                webTestClient.post().uri(authenticatedEP).cookie(jSessionIdCookieName, jSessionId)
                                .exchange().expectStatus().isUnauthorized();

                // Unregister user when not logged in
                webTestClient.post().uri(unregisterEP).cookie(jSessionIdCookieName, jSessionId)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue("password=" + rawPassword).exchange().expectStatus()
                                .isUnauthorized();

                // Log back in
                headers = webTestClient.post().uri(loginEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(emailFieldName + "=" + email + "&" + passwordFieldName
                                                + "=" + rawPassword)
                                .exchange().expectBody().returnResult().getResponseHeaders();

                System.out.println(String.valueOf(headers));
                setCookieHeader = headers.getFirst(HttpHeaders.SET_COOKIE);
                jSessionId = extractJSessionIdFromSetCookieHeader(setCookieHeader);
                System.out.println("Extracted JSESSIONID: " + jSessionId);

                // Unregister user with wrong cookie, password
                webTestClient.post().uri(unregisterEP)
                                .cookie(jSessionIdCookieName, "wrongJSessionId")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue("password=" + rawPassword).exchange().expectStatus()
                                .isUnauthorized();
                webTestClient.post().uri(unregisterEP).cookie(jSessionIdCookieName, jSessionId)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue("password=" + "wrongPassword").exchange().expectStatus()
                                .isBadRequest();
        }

}
