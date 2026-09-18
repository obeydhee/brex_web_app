package com.brex.demo.controller;

import com.brex.demo.dto.AuthResponse;
import com.brex.demo.dto.UserProfileResponse;
import com.brex.demo.model.UserProfile;
import com.brex.demo.security.JwtService;
import com.brex.demo.service.UserProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserProfileService userProfileService;
    private final JwtService jwtService;

    public AuthController(UserProfileService userProfileService, JwtService jwtService) {
        this.userProfileService = userProfileService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserProfile user = userProfileService.authenticate(request.identifier(), request.password());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserProfileResponse.from(user));
    }

    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
    }
}
