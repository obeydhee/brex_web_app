package com.brex.demo.dto;

import com.brex.demo.model.Account;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(BigDecimal balance, Instant updatedAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getBalance(), account.getUpdatedAt());
    }
}
