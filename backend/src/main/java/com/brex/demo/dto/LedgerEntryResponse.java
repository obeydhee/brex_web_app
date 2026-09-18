package com.brex.demo.dto;

import com.brex.demo.model.AccountLedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(Long id, String entryType, BigDecimal amount,
        BigDecimal balanceAfter, Instant createdAt) {

    public static LedgerEntryResponse from(AccountLedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getEntryType().name(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getCreatedAt());
    }
}
