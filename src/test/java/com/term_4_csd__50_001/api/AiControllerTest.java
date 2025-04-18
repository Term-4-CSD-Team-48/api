package com.term_4_csd__50_001.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.term_4_csd__50_001.api.services.AuthService;

public class AiControllerTest extends BaseTest {

        @Autowired
        private AuthService authService;

        @Test
        public void invocationsTest() throws Exception {
                String emailFieldName = "email";
                String passwordFieldName = "password";
                String email = "xyf.oco@gmail.com";
                String username = "username";
                String rawPassword = "password";
                authService.register(email, username, rawPassword, true);

                // Should throw 401
                webTestClient.post().uri(promptEP).contentType(MediaType.APPLICATION_JSON)
                                .bodyValue("{\"x\": 12.3, \"y\": 45.6}").exchange().expectStatus()
                                .isUnauthorized();

                // Should throw 401
                webTestClient.post().uri(observeEP).exchange().expectStatus().isUnauthorized();

                // Login with correct credentials
                HttpHeaders headers = webTestClient.post().uri(loginEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(emailFieldName + "=" + email + "&" + passwordFieldName
                                                + "=" + rawPassword)
                                .exchange().expectBody().returnResult().getResponseHeaders();

                // Get JSessionId cookie
                System.out.println(String.valueOf(headers));
                String setCookieHeader = headers.getFirst(HttpHeaders.SET_COOKIE);
                String jSessionId = extractJSessionIdFromSetCookieHeader(setCookieHeader);
                System.out.println("Extracted JSESSIONID: " + jSessionId);

                // Should return 503 (test is meant to run without AI server)
                webTestClient.post().uri(observeEP).cookie(jSessionIdCookieName, jSessionId)
                                .bodyValue(Map.of("token", "token")).exchange().expectStatus()
                                .isEqualTo(503);

                // Should throw 503 (test is meant to run without AI server)
                webTestClient.post().uri(promptEP).contentType(MediaType.APPLICATION_JSON)
                                .bodyValue("{\"x\": 12.3, \"y\": 45.6}")
                                .cookie(jSessionIdCookieName, jSessionId).exchange().expectStatus()
                                .isEqualTo(503);

        }

}
