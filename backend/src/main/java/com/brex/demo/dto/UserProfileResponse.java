package com.brex.demo.dto;

import com.brex.demo.model.UserProfile;
import java.time.Instant;

public record UserProfileResponse(Long id, String firstName, String lastName, String email,
        String phoneNumber, String paymentName, Instant createdAt) {

    public static UserProfileResponse from(UserProfile user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPaymentName(),
                user.getCreatedAt());
    }
}
