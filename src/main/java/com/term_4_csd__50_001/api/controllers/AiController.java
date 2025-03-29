package com.term_4_csd__50_001.api.controllers;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.term_4_csd__50_001.api.exceptions.BadRequestException;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;
import com.term_4_csd__50_001.api.services.AiService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/invocations")
    public void invocations(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        log.info("Received request at /invocations");
        if (!requestBody.containsKey("x") || !requestBody.containsKey("y")) {
            throw new BadRequestException("x and y need to be both present");
        }
        double x = (double) requestBody.get("x");
        double y = (double) requestBody.get("y");
        String jsessionId = request.getSession(false).getId();
        if (jsessionId.isBlank())
            // Should never happen as this EP is meant to be protected
            throw new InternalServerErrorException("Something went wrong");
        aiService.invocations(x, y, jsessionId);
    }

    @PostMapping("/assume-control")
    public void assumeControl(HttpServletRequest request) {
        log.info("Received request at /assume-control");
        String jsessionId = request.getSession(false).getId();
        if (jsessionId.isBlank())
            // Should never happen as this EP is meant to be protected
            throw new InternalServerErrorException("Something went wrong");
        aiService.assumeControl(jsessionId);
    }

}
