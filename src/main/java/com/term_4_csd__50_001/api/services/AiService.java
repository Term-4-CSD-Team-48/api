package com.term_4_csd__50_001.api.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.term_4_csd__50_001.api.Dotenv;
import com.term_4_csd__50_001.api.exceptions.ConflictException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService {

    private final String AI_INFERENCE_IP_ADDRESS;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private volatile boolean pingingAiServer = false;
    private volatile boolean aiServerHealthy = false;

    @Autowired
    public AiService(Dotenv dotenv) {
        AI_INFERENCE_IP_ADDRESS = dotenv.get(Dotenv.AI_INFERENCE_IP_ADDRESS);
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

    private String combineIpAndPort(String ip, int port) {
        return String.format("%s:%d", ip, port);
    }

}
