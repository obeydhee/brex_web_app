package com.brex.demo.controller;

import com.brex.demo.dto.TransactionResponse;
import com.brex.demo.model.Transaction;
import com.brex.demo.security.AuthenticatedUser;
import com.brex.demo.service.PaymentService;
import com.brex.demo.service.UserProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserProfileService userProfileService;

    public PaymentController(PaymentService paymentService, UserProfileService userProfileService) {
        this.paymentService = paymentService;
        this.userProfileService = userProfileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse sendPayment(@AuthenticationPrincipal(errorOnInvalidType = false) AuthenticatedUser principal,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody SendPaymentRequest request) {
        Long senderId = resolveUserId(principal, userId);
        Transaction tx = paymentService.transfer(senderId, request.receiverPaymentName(), request.amount(), request.note());
        String senderPaymentName = principal != null
                ? principal.paymentName()
                : userProfileService.findById(senderId).getPaymentName();
        String receiverPaymentName = userProfileService.findById(tx.getReceiverId()).getPaymentName();
        return TransactionResponse.from(tx, senderPaymentName, receiverPaymentName);
    }

    /**
     * Authentication enforcement is currently disabled (see SecurityConfig),
     * so the acting user comes from a validated JWT if one was supplied, and
     * otherwise falls back to an explicit ?userId= query param for easy
     * testing without tokens.
     */
    private Long resolveUserId(AuthenticatedUser principal, Long userId) {
        if (principal != null) {
            return principal.userId();
        }
        if (userId != null) {
            return userId;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No authenticated principal and no userId query param provided");
    }

    public record SendPaymentRequest(
            @NotBlank String receiverPaymentName,
            @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
            String note) {
    }
}
