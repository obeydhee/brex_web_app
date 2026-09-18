package com.brex.demo.security;

/**
 * The identity resolved from a validated JWT. Bound to controller method
 * parameters via {@code @AuthenticationPrincipal}.
 */
public record AuthenticatedUser(Long userId, String paymentName) {
}
