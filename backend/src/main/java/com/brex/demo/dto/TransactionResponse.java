package com.brex.demo.dto;

import com.brex.demo.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(Long id, Long senderId, String senderPaymentName,
        Long receiverId, String receiverPaymentName, BigDecimal amount,
        String status, String note, Instant createdAt, Instant updatedAt) {

    public static TransactionResponse from(Transaction tx, String senderPaymentName, String receiverPaymentName) {
        return new TransactionResponse(
                tx.getId(),
                tx.getSenderId(),
                senderPaymentName,
                tx.getReceiverId(),
                receiverPaymentName,
                tx.getAmount(),
                tx.getStatus().name(),
                tx.getNote(),
                tx.getCreatedAt(),
                tx.getUpdatedAt());
    }
}
