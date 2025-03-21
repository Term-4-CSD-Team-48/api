package com.term_4_csd__50_001.api.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.term_4_csd__50_001.api.Dotenv;
import com.term_4_csd__50_001.api.exceptions.ConflictException;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService {

    private final String AI_INFERENCE_IP_ADDRESS;
    private final CameraService cameraService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Use insertSubscription and removeSubscription to manipulate
     */
    private volatile Map<String, Boolean> subscriptions = new HashMap<>();
    private volatile boolean pingingAiServer = false;
    private volatile boolean aiServerHealthy = false;

    @Autowired
    public AiService(Dotenv dotenv, CameraService cameraService) {
        AI_INFERENCE_IP_ADDRESS = dotenv.get(Dotenv.AI_INFERENCE_IP_ADDRESS);
        this.cameraService = cameraService;
        pingAiServerRegularly();
    }

    private void pingAiServerRegularly() {
        if (isPingingAiServer())
            throw new ConflictException("Already pinging AI server");
        setPingingAiServer(true);
        executorService.submit(() -> {
            while (true) {
                try {
                    URI uri = new URI("http", null, AI_INFERENCE_IP_ADDRESS, 8080, "/ping", null,
                            null);
                    URL url = uri.toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int code = connection.getResponseCode();
                    if (code == 200) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()))) {
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            String body = response.toString();
                            log.debug("When pinged AI server, it responded with body " + body);
                            if ("healthy".equals(body)) {
                                setAiServerHealthy(true);
                            } else {
                                setAiServerHealthy(false);
                            }
                        }
                    } else {
                        log.error("AI server responded with code " + code + " when pinged");
                        setAiServerHealthy(false);
                    }
                    Thread.sleep(1000 * 60); // Wait before checking again
                } catch (InterruptedException e) {
                    setPingingAiServer(false);
                    setAiServerHealthy(false);
                    log.info("Subscription thread interrupted");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    setAiServerHealthy(false);
                    log.error("Error in subscription thread: " + e.getMessage());
                }
            }
        });
    }

    private void setAiServerHealthy(boolean aiServerHealthy) {
        this.aiServerHealthy = aiServerHealthy;
    }

    public boolean isAiServerHealthy() {
        return aiServerHealthy;
    }

    private void setPingingAiServer(boolean pingingAiServer) {
        this.pingingAiServer = pingingAiServer;
    }

    public boolean isPingingAiServer() {
        return pingingAiServer;
    }

    /**
     * Throws a ConflictException if subscription already exists for given ip and port. Use
     * combineIpAndPort to generate the param ipAndPort
     * 
     * @param ipAndPort
     */
    private void insertSubscription(String ipAndPort) {
        if (subscriptions.containsKey(ipAndPort))
            throw new ConflictException("Subscription already exists");
        subscriptions.put(ipAndPort, true);
    }

    /**
     * Use combineIpAndPort to generate the param ipAndPort
     * 
     * @param ipAndPort
     */
    private void removeSubscription(String ipAndPort) {
        subscriptions.remove(ipAndPort);
    }

    private String combineIpAndPort(String ip, int port) {
        return String.format("%s:%d", ip, port);
    }

    public void subscribe(String ip, int port) {
        if (!cameraService.isListening())
            throw new InternalServerErrorException("Not receiving anything from camera");
        String combined = combineIpAndPort(ip, port);
        insertSubscription(combined);
        log.info(String.format("Subscribing for %s:%d", ip, port));
        executorService.submit(() -> {
            String lastHash = null;
            int maxFPS = 30;
            int intervalMillis = 1000 / maxFPS;

            while (true) {
                try {
                    // Get the current frame data hash
                    String currentHash = cameraService.getFrameDataHash();

                    // If the hash has changed, process the camera data
                    if (!currentHash.equals(lastHash)) {
                        log.debug(String.format(
                                "Found new frame data! Previous hash was %s while new hash is %s",
                                lastHash, currentHash));
                        ResponseEntity<String> response = processCameraData(0, 0);
                        log.debug("Received response with " + response.getStatusCode());
                        lastHash = currentHash; // Update the last hash

                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(response.getBody());
                        String frameBase64 = rootNode.get("frame").asText();

                        byte[] jpegData = Base64.getDecoder().decode(frameBase64);

                        try (DatagramSocket socket = new DatagramSocket()) {
                            InetAddress address = InetAddress.getByName(ip);
                            DatagramPacket packet =
                                    new DatagramPacket(jpegData, jpegData.length, address, port);
                            socket.send(packet);
                            log.debug(String.format("Sent JPEG frame to %s:%d", ip, port));
                        }
                    }

                    Thread.sleep(intervalMillis); // Wait before checking again
                } catch (InterruptedException e) {
                    log.info("Subscription thread interrupted");
                    removeSubscription(combined);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Error in subscription thread: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 
     * @param x >= 0
     * @param y >= 0
     * @return
     */
    public ResponseEntity<String> processCameraData(int x, int y) {
        byte[] frameData = cameraService.getFrameData();

        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create the body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(frameData) {
            @Override
            public String getFilename() {
                return "frame.jpg";
            }
        });
        String prompt = String.format("{'x': %d, 'y': %d}", x < 0 ? 0 : x, y < 0 ? 0 : y);
        body.add("prompt", prompt);

        // Create request entity
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Configure RestTemplate with timeout
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000); // 1 seconds connection timeout
        requestFactory.setReadTimeout(1000); // 1 seconds read timeout
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        // Send the request
        String url = "http://" + AI_INFERENCE_IP_ADDRESS + ":8080/invocations";
        log.debug(String.format("Sending frame to ai at %s", url));
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, requestEntity, String.class);
        return response;
    }

}
