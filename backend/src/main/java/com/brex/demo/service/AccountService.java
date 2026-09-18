package com.brex.demo.service;

import com.brex.demo.model.Account;
import com.brex.demo.model.AccountLedgerEntry;
import com.brex.demo.model.AccountLedgerEntry.EntryType;
import com.brex.demo.model.UserProfile;
import com.brex.demo.repository.AccountLedgerEntryRepository;
import com.brex.demo.repository.AccountRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountLedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accountRepository, AccountLedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public Account provisionZeroBalanceAccount(UserProfile userProfile) {
        return accountRepository.save(new Account(userProfile));
    }

    public Account getAccount(Long userId) {
        return accountRepository.findByUserProfileId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No account for user: " + userId));
    }

    @Transactional
    public Account deposit(Long userId, BigDecimal amount) {
        requirePositiveAmount(amount);
        Account account = getAccount(userId);
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        account = saveWithLockCheck(account);
        ledgerEntryRepository.save(new AccountLedgerEntry(account, EntryType.DEPOSIT, amount, newBalance));
        return account;
    }

    @Transactional
    public Account withdraw(Long userId, BigDecimal amount) {
        requirePositiveAmount(amount);
        Account account = getAccount(userId);
        if (amount.compareTo(account.getBalance()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        account = saveWithLockCheck(account);
        ledgerEntryRepository.save(new AccountLedgerEntry(account, EntryType.WITHDRAWAL, amount, newBalance));
        return account;
    }

    public Page<AccountLedgerEntry> getLedger(Long userId, Pageable pageable) {
        Account account = getAccount(userId);
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable);
    }

    /**
     * Forces an immediate flush (rather than letting Hibernate defer the UPDATE
     * to transaction commit) so an {@code @Version} conflict surfaces here,
     * inside the service call, where it can be translated into a 409 — instead
     * of escaping as an unhandled exception after the method has already
     * returned.
     */
    private Account saveWithLockCheck(Account account) {
        try {
            return accountRepository.saveAndFlush(account);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Account was modified concurrently, please retry", e);
        }
    }

    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
    }
}
