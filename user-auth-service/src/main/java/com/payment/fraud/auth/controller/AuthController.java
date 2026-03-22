package com.payment.fraud.auth.controller;


import com.payment.fraud.auth.dto.AuthRequest;
import com.payment.fraud.auth.dto.AuthResponse;
import com.payment.fraud.auth.dto.RegisterRequest;
import com.payment.fraud.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {

        System.out.println("Login endpoint hit!");
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
