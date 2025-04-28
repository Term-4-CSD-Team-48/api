package com.term_4_csd__50_001.api.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.term_4_csd__50_001.api.Dotenv;
import com.term_4_csd__50_001.api.exceptions.ConflictException;
import com.term_4_csd__50_001.api.exceptions.ForbiddenException;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;
import com.term_4_csd__50_001.api.exceptions.ServiceUnavailableException;
import com.term_4_csd__50_001.api.exceptions.UnauthorizedRequestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AIService {

    private final String AI_SERVER_URL;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private FCMService fcmService;

    private volatile boolean pingingAiServer = false;
    private volatile boolean aiServerHealthy = false;

    private String observerJSessionId = "";
    private String observerFCMToken = "";
    private String observerPrincipal = "";

    @Autowired
    public AIService(Dotenv dotenv) {
        AI_SERVER_URL = dotenv.get(Dotenv.AI_SERVER_URL);
        pingAiServerRegularly();
    }

    public void prompt(float x, float y, String jSessionId) {
        if (!isAiServerHealthy())
            throw new ServiceUnavailableException("AI server is down");
        if (observerJSessionId.isBlank())
            throw new UnauthorizedRequestException("Please observe AI at /observe");
        if (jSessionId != observerJSessionId)
            throw new ForbiddenException("Someone else already observing");
        try {
            URI baseUri = new URI(AI_SERVER_URL); // Parse the AI_SERVER_URL into a URI object
            URI uri = new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(),
                    "/prompt", null, null); // Use the components of the baseUri to construct the
                                            // new URI
            URL url = uri.toURL(); // Convert the URI to a URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            String jsonInputString = String.format("{\"x\": %f, \"y\": %f}", x, y);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int code = connection.getResponseCode();
            log.info(String.format("Received response code %d from /prompt", code));
            switch (code) {
                case 200:
                    return;
                default:
                    throw new InternalServerErrorException("Could not communicate with ai");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new InternalServerErrorException("Something went wrong", e);
        }
    }

    public void observe(String jSessionId, String fcmToken, String principal) {
        if (!isAiServerHealthy())
            throw new ServiceUnavailableException("AI server is down");

        // Only when observerPrincipal is empty can a new observer observe
        // If principals are not same it means different observer so deny them
        // Next check if jSessionId are the same. If not means need to inform AI server
        // of the change in jSessionID for the observer
        if (!observerPrincipal.isBlank()) {
            if (!principal.equals(observerPrincipal)) {
                throw new ConflictException("Someone else already observing");
            } else {
                if (observerJSessionId.equals(jSessionId)) {
                    return;
                }
            }
        }
        try {
            URI baseUri = new URI(AI_SERVER_URL); // Parse AI_SERVER_URL into a URI object
            URI uri = new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(),
                    "/observe", null, null); // Use the components of baseUri to construct the new
                                             // URI
            URL url = uri.toURL(); // Convert the URI to a URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true); // Enable output for the connection
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            String jsonInputString = String.format("{\"jSessionId\": \"%s\"}", jSessionId);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int code = connection.getResponseCode();
            log.info("Received response code {} from /observe", code);
            switch (code) {
                case 400:
                    throw new InternalServerErrorException(
                            "AI server did not interpret request correctly");
                case 403:
                    throw new InternalServerErrorException(
                            "AI server misclassified API as outsider");
                case 200:
                    observerFCMToken = fcmToken;
                    observerJSessionId = jSessionId;
                    observerPrincipal = principal;
                    return;
                default:
                    throw new InternalServerErrorException(
                            "An unknown error occured in sending request to AI server");
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Could not communicate with AI server", e);
        }
    }

    public void onUpdate(Boolean objectOnScreen) {
        if (observerFCMToken.isBlank())
            throw new InternalServerErrorException("No FCM token to push");
        fcmService.sendNotifcation(observerFCMToken, "Status update",
                objectOnScreen ? "Parcel is being tracked" : "Parcel is not being tracked");
    }

    private void pingAiServer() throws URISyntaxException, MalformedURLException, IOException {
        URI uri = new URI("http", null, AI_SERVER_URL, 8080, "/ping", null, null);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        if (code == 200) {
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                String body = response.toString();
                log.info("When pinged AI server, it responded with body " + body);
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
    }

    private void pingAiServerRegularly() {
        if (isPingingAiServer())
            throw new ConflictException("Already pinging AI server");
        setPingingAiServer(true);
        executorService.submit(() -> {
            while (true) {
                try {
                    pingAiServer();
                    Thread.sleep(1000 * 60); // Wait before checking again
                } catch (InterruptedException e) {
                    setPingingAiServer(false);
                    setAiServerHealthy(false);
                    log.info("Ping AI server thread interrupted");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    setAiServerHealthy(false);
                    log.error("Error in ping AI server thread: " + e.getMessage());
                    Thread.sleep(1000 * 60); // Wait before checking again
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

}
