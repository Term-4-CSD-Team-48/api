package com.term_4_csd__50_001.api;

import org.springframework.stereotype.Service;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;

/**
 * Dotenv will first check System.getenv and use the .env as fallback
 */
@Service
public class Dotenv {

    private final io.github.cdimascio.dotenv.Dotenv dotenv;

    public static final String AI_INFERENCE_IP_ADDRESS = "AI_INFERENCE_IP_ADDRESS";
    public static final String CAMERA_SECRET = "CAMERA_SECRET";
    public static final String MONGO_CONNECTION_STRING = "MONGO_CONNECTION_STRING";

    Dotenv() {
        dotenv = io.github.cdimascio.dotenv.Dotenv.load();
    }

    /**
     * @param key obtained from static properties of the class like Dotenv.MONGO_CONNECTION_STRING
     * @return the value as a string
     */
    public String get(String key) throws InternalServerErrorException {
        String result = System.getenv(key); // First, check system environment variables
        if (result == null) // If not found, fall back to .env file
            result = dotenv.get(key, null);
        if (result == null)
            throw new InternalServerErrorException(String.format("Key %s does not exist", key));
        return result;
    }

}
