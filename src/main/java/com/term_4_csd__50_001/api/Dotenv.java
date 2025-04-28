package com.term_4_csd__50_001.api;

import org.springframework.stereotype.Service;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;

/**
 * Dotenv will first check System.getenv and use the .env as fallback
 */
@Slf4j
@Service
public class Dotenv {

    private io.github.cdimascio.dotenv.Dotenv dotenv;

    public static final String AI_SERVER_URL = "AI_SERVER_URL";
    public static final String MONGO_CONNECTION_STRING = "MONGO_CONNECTION_STRING";
    public static final String MAIL_HOST = "MAIL_HOST";
    public static final String MAIL_PORT = "MAIL_PORT";
    public static final String MAIL_USERNAME = "MAIL_USERNAME";
    public static final String MAIL_PASSWORD = "MAIL_PASSWORD";
    public static final String MAIL_SMTP_AUTH = "MAIL_SMTP_AUTH";
    public static final String MAIL_SMTP_STARTTLS = "MAIL_SMTP_STARTTLS";

    Dotenv() {
        try {
            dotenv = io.github.cdimascio.dotenv.Dotenv.load();
        } catch (Exception e) {
            log.error(
                    "Something went wrong while loading .env file. This can be safely ignored during runtime if you have set the required .env variables, otherwise the get method will throw an error if the required variables cannot be found.");
        }
    }

    /**
     * @param key obtained from static properties of the class like Dotenv.MONGO_CONNECTION_STRING
     * @return the value as a string
     */
    public String get(String key) throws InternalServerErrorException {
        String result = System.getenv(key); // First, check system environment variables
        if (result == null && dotenv != null) // If not found, fall back to .env file
            result = dotenv.get(key, null);
        if (result == null)
            throw new InternalServerErrorException(String.format("Key %s does not exist", key));
        return result;
    }

}
