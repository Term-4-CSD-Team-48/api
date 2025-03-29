package com.term_4_csd__50_001.api;

import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.term_4_csd__50_001.api.collections.UserCollection;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class BaseTest {

    @Autowired
    protected UserCollection userCollection;

    @Autowired
    protected Dotenv dotenv;

    @Autowired
    private Database database;

    @Autowired
    protected WebTestClient webTestClient;

    final protected String authEP = "/auth";
    final protected String authenticatedEP = authEP + "/authenticated";
    final protected String loginEP = authEP + "/login";
    final protected String logoutEP = authEP + "/logout";
    final protected String registerEP = authEP + "/register";
    final protected String unregisterEP = authEP + "/unregister";
    final protected String verifyEmailEP = authEP + "/verify-email";

    final protected String cameraEP = "/camera";
    final protected String cameraStartListeningEP = cameraEP + "/start-listening";

    final protected String cameraSecretFieldName = "camera_secret";
    final protected String sharedSecretFieldName = "shared_secret";
    final protected String sharedSecret = "QUJDREVGR0hJSktMTU4wMQ==";
    final protected String urlEncodedSharedSecret = "QUJDREVGR0hJSktMTU4wMQ%3D%3D";
    final protected byte[] sharedSecretBytes = Base64.getDecoder().decode(sharedSecret);

    final protected String aiEP = "/ai";
    final protected String assumeControlEP = aiEP + "/assume-control";
    final protected String invocationsEP = aiEP + "/invocations";

    final protected String emailVerificationTokenURLParam = "email-verification-token";

    final protected String jSessionIdCookieName = "JSESSIONID";

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
                if (cookie.trim().startsWith(jSessionIdCookieName)) {
                    jSessionId = cookie.split("=")[1];
                    break;
                }
            }
        }
        if (jSessionId == "") {
            throw new IllegalArgumentException(String.format("Could not find %s in %s",
                    jSessionIdCookieName, setCookieHeader));
        }
        return jSessionId;
    }

}
