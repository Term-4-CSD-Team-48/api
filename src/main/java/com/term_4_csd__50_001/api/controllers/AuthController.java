package com.term_4_csd__50_001.api.controllers;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.term_4_csd__50_001.api.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/authenticated")
    public void authenticated() {}

    @PostMapping("/login")
    public void login(HttpServletResponse response) {
        response.setStatus(501);
        try {
            response.getWriter().write("Not implemented");
            response.getWriter().flush();
        } catch (IOException e) {
        }
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            SecurityContextHolder.clearContext();
        }
    }

    @PostMapping("/unregister")
    public void unregister(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object principal = session.getAttribute("SPRING_SECURITY_CONTEXT");
        Authentication authentication = ((SecurityContextImpl) principal).getAuthentication();
        String email = (String) authentication.getPrincipal();
        String password = request.getParameter("password");
        authService.unregister(email, password);
        session.invalidate();
        SecurityContextHolder.clearContext();
    }

    @PostMapping("/register")
    public void register(HttpServletRequest request) {
        String email = request.getParameter("email");
        String username = request.getParameter("username");
        String rawPassword = request.getParameter("password");
        log.debug(String.format(
                "At /register obtained fields for\nEmail: %s\nUsername: %s\nPassword: %s", email,
                username, (rawPassword != null && !rawPassword.isEmpty()) ? "[RAWPASSWORD RECEIVED]"
                        : "[RAWPASSWORD EMPTY OR NULL]"));
        authService.register(email, username, rawPassword);
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam("email-verification-token") String token) {
        authService.verifyEmail(token);
    }

}
