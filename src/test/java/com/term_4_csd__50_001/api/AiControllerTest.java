package com.term_4_csd__50_001.api;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.term_4_csd__50_001.api.services.AuthService;

public class AiControllerTest extends BaseTest {

        @Autowired
        private AuthService authService;

        @Test
        public void subscribeTest() throws Exception {
                String emailFieldName = "email";
                String passwordFieldName = "password";
                String email = "xyf.oco@gmail.com";
                String username = "username";
                String rawPassword = "password";
                authService.register(email, username, rawPassword, true);

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

                // Register camera
                // Not needed to start listening as CameraController doesn't accept /start-listening
                // requests and CameraServices will automatically ping the camera to listen
                /**
                 * final String cameraSecret = dotenv.get(Dotenv.CAMERA_SECRET);
                 * webTestClient.post().uri(cameraStartListeningEP)
                 * .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                 * .bodyValue(cameraSecretFieldName + "=" + cameraSecret + "&" +
                 * sharedSecretFieldName + "=" + urlEncodedSharedSecret)
                 * .exchange().expectStatus().isOk();
                 */

        }

}
