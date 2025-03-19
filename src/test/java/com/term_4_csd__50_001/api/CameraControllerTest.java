package com.term_4_csd__50_001.api;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public class CameraControllerTest extends BaseTest {

        // @Test
        public void startListeningTest() throws Exception {
                final String cameraSecret = dotenv.get(Dotenv.CAMERA_SECRET);
                // Send an invalid cameraSecret
                webTestClient.post().uri(cameraStartListeningEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(cameraSecretFieldName + "=" + "wrong").exchange()
                                .expectStatus().isUnauthorized();

                // Send an invalid sharedSecret
                webTestClient.post().uri(cameraStartListeningEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(cameraSecretFieldName + "=" + cameraSecret + "&"
                                                + sharedSecretFieldName + "=wrong")
                                .exchange().expectStatus().isBadRequest();

                // Register camera
                webTestClient.post().uri(cameraStartListeningEP)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(cameraSecretFieldName + "=" + cameraSecret + "&"
                                                + sharedSecretFieldName + "=" + sharedSecret)
                                .exchange().expectStatus().isOk();

                // CRITICAL: The following test will never throw an error. Please check
                // console to verify that UDP has been sent and received.
                // One way is to stop all other tests from running or someone
                // please finally make this test testable.
                String frameData = "frame_data";
                String payload = sharedSecret + frameData;
                DatagramSocket socket = new DatagramSocket();
                InetAddress address = InetAddress.getByName("localhost");
                int port = 5000;
                DatagramPacket packet = new DatagramPacket(payload.getBytes(), payload.length(),
                                address, port);
                Thread.sleep(100); // Packet can get sent before the server starts listening
                socket.send(packet);
                System.out.println("Sent UDP packet " + frameData + " to " + address + ":" + port
                                + " at " + LocalDateTime.now());
                socket.close();
        }

}
