package com.brex.demo.controller;

import com.brex.demo.dto.AuthResponse;
import com.brex.demo.dto.UserLookupResponse;
import com.brex.demo.dto.UserProfileResponse;
import com.brex.demo.model.UserProfile;
import com.brex.demo.security.AuthenticatedUser;
import com.brex.demo.security.JwtService;
import com.brex.demo.service.UserProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final JwtService jwtService;

    public UserProfileController(UserProfileService userProfileService, JwtService jwtService) {
        this.userProfileService = userProfileService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse createUser(@Valid @RequestBody CreateUserProfileRequest request) {
        UserProfile user = userProfileService.register(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.paymentName(),
                request.password());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserProfileResponse.from(user));
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(@AuthenticationPrincipal(errorOnInvalidType = false) AuthenticatedUser principal,
            @RequestParam(required = false) Long userId) {
        Long resolvedId = principal != null ? principal.userId() : userId;
        if (resolvedId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No authenticated principal and no userId query param provided");
        }
        return UserProfileResponse.from(userProfileService.findById(resolvedId));
    }

    @GetMapping("/lookup")
    public UserLookupResponse lookupUser(@RequestParam String paymentName) {
        return UserLookupResponse.from(userProfileService.findByPaymentName(paymentName));
    }

    public record CreateUserProfileRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotBlank String phoneNumber,
            @NotBlank @Pattern(regexp = "^[a-z0-9_]{3,20}$") String paymentName,
            @NotBlank String password) {
    }
}
