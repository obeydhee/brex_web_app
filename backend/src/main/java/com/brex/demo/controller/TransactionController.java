package com.brex.demo.controller;

import com.brex.demo.dto.TransactionResponse;
import com.brex.demo.model.Transaction;
import com.brex.demo.security.AuthenticatedUser;
import com.brex.demo.service.PaymentService;
import com.brex.demo.service.UserProfileService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final PaymentService paymentService;
    private final UserProfileService userProfileService;

    public TransactionController(PaymentService paymentService, UserProfileService userProfileService) {
        this.paymentService = paymentService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public Page<TransactionResponse> getHistory(@AuthenticationPrincipal(errorOnInvalidType = false) AuthenticatedUser principal,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String window,
            @RequestParam(defaultValue = "0") int page) {
        return paymentService.getHistory(resolveUserId(principal, userId), window, page)
                .map(this::toResponse);
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@AuthenticationPrincipal(errorOnInvalidType = false) AuthenticatedUser principal,
            @RequestParam(required = false) Long userId,
            @PathVariable Long id) {
        Transaction tx = paymentService.getById(resolveUserId(principal, userId), id);
        return toResponse(tx);
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

    private TransactionResponse toResponse(Transaction tx) {
        String senderPaymentName = userProfileService.findById(tx.getSenderId()).getPaymentName();
        String receiverPaymentName = userProfileService.findById(tx.getReceiverId()).getPaymentName();
        return TransactionResponse.from(tx, senderPaymentName, receiverPaymentName);
    }
}
