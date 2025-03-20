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
                final String cameraSecret = dotenv.get(Dotenv.CAMERA_SECRET);
                webTestClient.post().uri(cameraStartListeningEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(cameraSecretFieldName + "=" + cameraSecret + "&"
                                                + sharedSecretFieldName + "="
                                                + urlEncodedSharedSecret)
                                .exchange().expectStatus().isOk();

                // CRITICAL: The following test will never throw an error. Please check
                // console to verify that UDP has been sent and received.
                // One way is to stop all other tests from running or someone
                // please finally make this test testable.
                webTestClient.post().uri(subscribeEP).cookie(jSessionIdCookieName, jSessionId)
                                .exchange().expectStatus().isOk();
                String frameData = "frame_data";
                byte[] frameDataBytes = frameData.getBytes();
                byte[] payload = new byte[sharedSecretBytes.length + frameDataBytes.length];
                System.arraycopy(sharedSecretBytes, 0, payload, 0, sharedSecretBytes.length);
                System.arraycopy(frameDataBytes, 0, payload, sharedSecretBytes.length,
                                frameDataBytes.length);
                DatagramSocket socket = new DatagramSocket();
                InetAddress address = InetAddress.getByName("localhost");
                int port = 5000;
                for (int i = 0; i < 10; i++) {
                        frameData = "frame_data" + i;
                        frameDataBytes = frameData.getBytes();
                        payload = new byte[sharedSecretBytes.length + frameDataBytes.length];
                        System.arraycopy(sharedSecretBytes, 0, payload, 0,
                                        sharedSecretBytes.length);
                        System.arraycopy(frameDataBytes, 0, payload, sharedSecretBytes.length,
                                        frameDataBytes.length);
                        DatagramPacket packet =
                                        new DatagramPacket(payload, payload.length, address, port);
                        Thread.sleep(100);
                        socket.send(packet);
                        System.out.println("Sent UDP packet " + frameData + " to " + address + ":"
                                        + port + " at " + LocalDateTime.now());
                }
                socket.close();
        }

}
