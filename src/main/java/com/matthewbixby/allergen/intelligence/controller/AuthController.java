package com.matthewbixby.allergen.intelligence.controller;

import com.matthewbixby.allergen.intelligence.dto.AuthenticationRequest;
import com.matthewbixby.allergen.intelligence.dto.AuthenticationResponse;
import com.matthewbixby.allergen.intelligence.dto.RegisterRequest;
import com.matthewbixby.allergen.intelligence.model.User;
import com.matthewbixby.allergen.intelligence.repository.UserRepository;
import com.matthewbixby.allergen.intelligence.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email already registered");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setTotalTokensUsed(0);
        user.setAnalysesRun(0);

        userRepository.save(user);

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String jwtToken = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .token(jwtToken)
                    .email(request.getEmail())
                    .build());

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractUsername(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("totalTokensUsed", user.getTotalTokensUsed());
            userData.put("analysesRun", user.getAnalysesRun());
            userData.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}