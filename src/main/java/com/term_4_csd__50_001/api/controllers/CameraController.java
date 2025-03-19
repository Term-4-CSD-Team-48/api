package com.term_4_csd__50_001.api.controllers;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.term_4_csd__50_001.api.Dotenv;
import com.term_4_csd__50_001.api.exceptions.BadRequestException;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;
import com.term_4_csd__50_001.api.exceptions.UnauthorizedRequestException;
import com.term_4_csd__50_001.api.services.CameraService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/camera")
public class CameraController {

    private final String CAMERA_SECRET;

    @Autowired
    private CameraService cameraService;

    @Autowired
    public CameraController(Dotenv dotenv) {
        CAMERA_SECRET = dotenv.get(Dotenv.CAMERA_SECRET);
    }

    @PostMapping("/start-listening")
    public void register(HttpServletRequest request) {
        // Input checking
        String cameraSecret = request.getParameter("camera_secret");
        if (!cameraSecret.equals(CAMERA_SECRET))
            throw new UnauthorizedRequestException("Provided camera secret is not valid");
        String sharedSecret = request.getParameter("shared_secret");
        if (sharedSecret == null)
            throw new BadRequestException("Please provide a shared secret");
        int sharedSecretUTF8ByteLength = sharedSecret.getBytes().length;
        log.info("Request received at /start-listening and shared secret has byte length (UTF-8): "
                + sharedSecretUTF8ByteLength);
        if (sharedSecret.getBytes().length != 16)
            throw new BadRequestException(
                    "Please provide a shared secret that will be 16 bytes long when encoded in UTF-8");

        // Start listening on port 5000
        try {
            cameraService.startListening(sharedSecret);
        } catch (InterruptedException e) {
            throw new InternalServerErrorException("Something went wrong!");
        }
    }

}
