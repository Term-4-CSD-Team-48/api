package com.term_4_csd__50_001.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class BaseTest {
    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    private Database database;

    final protected String authEP = "/auth";
    final protected String authenticatedEP = authEP + "/authenticated";
    final protected String loginEP = authEP + "/login";
    final protected String logoutEP = authEP + "/logout";
    final protected String registerEP = authEP + "/register";
    final protected String unregisterEP = authEP + "/unregister";
    final protected String verifyEmailEP = authEP + "/verify-email";

    final protected String emailVerificationTokenURLParam = "email-verification-token";

    final protected String jSessionIdFieldName = "JSESSIONID";

    @BeforeEach
    public void beforeEach() {
        database.cleanDatabase();
    }

    @AfterEach
    public void afterEach() {
        database.cleanDatabase();
    }

    protected String extractJSessionIdFromSetCookieHeader(String setCookieHeader)
            throws IllegalArgumentException {
        String jSessionId = "";
        if (setCookieHeader != null) {
            for (String cookie : setCookieHeader.split(";")) {
                if (cookie.trim().startsWith(jSessionIdFieldName)) {
                    jSessionId = cookie.split("=")[1];
                    break;
                }
            }
        }
        if (jSessionId == "") {
            throw new IllegalArgumentException(
                    String.format("Could not find %s in %s", jSessionIdFieldName, setCookieHeader));
        }
        return jSessionId;
    }

}
