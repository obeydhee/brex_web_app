package com.brex.demo.controller;

import com.brex.demo.dto.AccountResponse;
import com.brex.demo.dto.LedgerEntryResponse;
import com.brex.demo.model.Account;
import com.brex.demo.security.AuthenticatedUser;
import com.brex.demo.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public AccountResponse getCurrentAccount(@AuthenticationPrincipal(errorOnInvalidType = false) AuthenticatedUser principal,
            @RequestParam(required = false) Long userId) {
        return AccountResponse.from(accountService.getAccount(resolveUserId(principal, userId)));
    }

    @PostMapping("/me/deposit")
    public AccountResponse deposit(@AuthenticationPrincipal(errorOnInvalidType = false) AuthenticatedUser principal,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody AmountRequest request) {
        Account account = accountService.deposit(resolveUserId(principal, userId), request.amount());
        return AccountResponse.from(account);
    }

    @PostMapping("/me/withdraw")
    public AccountResponse withdraw(@AuthenticationPrincipal(errorOnInvalidType = false) AuthenticatedUser principal,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody AmountRequest request) {
        Account account = accountService.withdraw(resolveUserId(principal, userId), request.amount());
        return AccountResponse.from(account);
    }

    @GetMapping("/me/ledger")
    public Page<LedgerEntryResponse> getLedger(@AuthenticationPrincipal(errorOnInvalidType = false) AuthenticatedUser principal,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page) {
        return accountService.getLedger(resolveUserId(principal, userId), PageRequest.of(page, 15))
                .map(LedgerEntryResponse::from);
    }

    /**
     * Authentication enforcement is currently disabled (see SecurityConfig),
     * so the acting user comes from a validated JWT if one was supplied, and
     * otherwise falls back to an explicit ?userId= query param for easy
     * testing without tokens. Re-enabling enforcement makes principal always
     * non-null, at which point this fallback simply stops being exercised.
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

    public record AmountRequest(
            @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {
    }
}
