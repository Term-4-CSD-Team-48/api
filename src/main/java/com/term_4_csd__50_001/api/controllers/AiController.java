package com.term_4_csd__50_001.api.controllers;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.term_4_csd__50_001.api.exceptions.BadRequestException;
import com.term_4_csd__50_001.api.exceptions.ForbiddenException;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;
import com.term_4_csd__50_001.api.services.AIService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/prompt")
    public void invocations(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        if (!requestBody.containsKey("x") || !requestBody.containsKey("y")) {
            throw new BadRequestException("x and y need to be both present");
        }
        float x = ((Number) requestBody.get("x")).floatValue();
        float y = ((Number) requestBody.get("y")).floatValue();
        String jsessionId = request.getSession(false).getId();
        if (jsessionId.isBlank())
            // Should never happen as this EP is meant to be protected
            throw new InternalServerErrorException("Something went wrong");
        aiService.prompt(x, y, jsessionId);
    }

    @PostMapping("/observe")
    public void observe(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        String jsessionId = request.getSession(false).getId();
        if (jsessionId.isBlank())
            // Should never happen as this EP is meant to be protected
            throw new InternalServerErrorException("Something went wrong");
        String fcmToken = (String) requestBody.get("token");
        if (fcmToken.isBlank())
            throw new BadRequestException("need token");
        aiService.observe(jsessionId, fcmToken);
    }

    @PostMapping("/on-update")
    public void onUpdate(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        String clientIP = request.getRemoteAddr();
        if (!clientIP.startsWith("10.0.") && !clientIP.startsWith("192.168."))
            throw new ForbiddenException("Outsiders not allowed");
        Boolean objectOnScreen = (Boolean) requestBody.get("objectOnScreen");
        aiService.onUpdate(objectOnScreen);
    }
}
