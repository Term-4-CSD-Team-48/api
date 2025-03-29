package com.term_4_csd__50_001.api.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
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
public class AiService {

    private final String AI_INFERENCE_IP_ADDRESS;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private volatile boolean pingingAiServer = false;
    private volatile boolean aiServerHealthy = false;

    private String ownerJSessionId = "";

    @Autowired
    public AiService(Dotenv dotenv) {
        AI_INFERENCE_IP_ADDRESS = dotenv.get(Dotenv.AI_INFERENCE_IP_ADDRESS);
        pingAiServerRegularly();
    }

    public void invocations(double x, double y, String jSessionId) {
        if (!isAiServerHealthy())
            throw new ServiceUnavailableException("AI server is down");
        if (ownerJSessionId.isBlank())
            throw new UnauthorizedRequestException(
                    "Please assume control of the AI at /assume-control");
        if (jSessionId != ownerJSessionId)
            throw new ForbiddenException("Someone else already assumed control");
        try {
            URI uri = new URI("http", null, AI_INFERENCE_IP_ADDRESS, 8080, "/invocations", null,
                    null);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            String jsonInputString = String.format("{\"x\": %f, \"y\": %f}", x, y);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes();
                os.write(input, 0, input.length);
            }
            int code = connection.getResponseCode();
            log.info(String.format("Received response code %d from /invocations", code));
            switch (code) {
                case 200:
                    return;
                default:
                    throw new InternalServerErrorException("Could not communicate with ai");
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Something went wrong", e);
        }
    }

    public void assumeControl(String jSessionId) {
        if (!isAiServerHealthy())
            throw new ServiceUnavailableException("AI server is down");
        if (jSessionId.equals(ownerJSessionId))
            return;
        if (!ownerJSessionId.isBlank())
            throw new ConflictException("Someone else already assumed control");
        try {
            URI uri =
                    new URI("http", null, AI_INFERENCE_IP_ADDRESS, 8080, "/set-owner", null, null);
            URL url = uri.toURL();
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
            log.info("Received response code {} from /set-owner", code);
            switch (code) {
                case 400:
                    throw new InternalServerErrorException(
                            "AI server did not interpret request correctly");
                case 403:
                    throw new InternalServerErrorException(
                            "AI server misclassified API as outsider");
                case 200:
                    ownerJSessionId = jSessionId;
                    return;
                default:
                    throw new InternalServerErrorException(
                            "An unknown error occured in sending request to AI server");
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Could not communicate with AI server", e);
        }
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
                    Thread.sleep(1000 * 60); // Wait before checking again
                } catch (InterruptedException e) {
                    setPingingAiServer(false);
                    setAiServerHealthy(false);
                    log.info("Subscription thread interrupted");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    setAiServerHealthy(false);
                    log.error("Error in subscription thread: " + e.getMessage());
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
