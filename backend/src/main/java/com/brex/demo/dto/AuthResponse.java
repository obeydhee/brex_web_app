package com.brex.demo.dto;

public record AuthResponse(String token, UserProfileResponse profile) {
}
