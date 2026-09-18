package com.brex.demo.dto;

import com.brex.demo.model.UserProfile;

public record UserLookupResponse(Long id, String paymentName, String firstName, String lastName) {

    public static UserLookupResponse from(UserProfile user) {
        return new UserLookupResponse(
                user.getId(),
                user.getPaymentName(),
                user.getFirstName(),
                user.getLastName());
    }
}
