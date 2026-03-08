package com.cheapradar.backend.controller;

import com.cheapradar.backend.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // Mocking authentication check
        if ("test".equals(username) && "test".equals(password)) {
            String jwt = jwtUtils.generateTokenFromUsername(username);
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "username", username,
                    "roles", "ROLE_USER"
            ));
        }

        return ResponseEntity.status(401).body("Error: Unauthorized");
    }
}
