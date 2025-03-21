package com.term_4_csd__50_001.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.term_4_csd__50_001.api.services.AiService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/subscribe")
    public void subscribe(HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        int port = request.getRemotePort();
        log.info(String.format("Received request from %s:%d", ipAddress, port));
        aiService.subscribe(ipAddress, port);
    }

}
