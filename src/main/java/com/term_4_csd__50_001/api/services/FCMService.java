package com.term_4_csd__50_001.api.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.springframework.stereotype.Service;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FCMService {

    public FCMService() throws FileNotFoundException, IOException {
        FileInputStream serviceAccount = new FileInputStream("service-account-key.json");
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
        if (FirebaseApp.getApps().isEmpty())
            FirebaseApp.initializeApp(options);
    }


    public void sendNotifcation(String token, String title, String body) {
        Message message = Message.builder().setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();
        String response;
        try {
            response = FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
        log.info("Sent message: " + response);
    }

}
