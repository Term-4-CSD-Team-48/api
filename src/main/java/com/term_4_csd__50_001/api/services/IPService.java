package com.term_4_csd__50_001.api.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import org.springframework.stereotype.Service;

@Service
public class IPService {

    private String cachedSelfPublicIp = "";

    public IPService() {
        cachedSelfPublicIp = getSelfPublicIP();
    }

    public String getSelfPublicIP() {
        if (!cachedSelfPublicIp.isEmpty())
            return cachedSelfPublicIp;
        String publicIP = "";
        try {
            URI uri = new URI("https", null, "api.ipify.org", 443, "/", null, null);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                publicIP = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cachedSelfPublicIp = publicIP;
        return publicIP;
    }

}
