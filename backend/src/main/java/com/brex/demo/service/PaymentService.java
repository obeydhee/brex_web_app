package com.brex.demo.service;

import com.brex.demo.model.Account;
import com.brex.demo.model.Transaction;
import com.brex.demo.model.Transaction.Status;
import com.brex.demo.model.UserProfile;
import com.brex.demo.repository.AccountRepository;
import com.brex.demo.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private static final int PAGE_SIZE = 15;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserProfileService userProfileService;

    public PaymentService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            UserProfileService userProfileService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userProfileService = userProfileService;
    }

    @Transactional
    public Transaction transfer(Long senderId, String receiverPaymentName, BigDecimal amount, String note) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        UserProfile receiver = userProfileService.findByPaymentName(receiverPaymentName);
        if (receiver.getId().equals(senderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pay yourself");
        }

        Account senderAccount = accountRepository.findByUserProfileId(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account for sender"));
        Account receiverAccount = accountRepository.findByUserProfileId(receiver.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account for receiver"));

        if (senderAccount.getBalance().compareTo(amount) < 0) {
            Transaction failed = new Transaction(senderId, receiver.getId(), amount, Status.FAILURE, note);
            return transactionRepository.save(failed);
        }

        // Always lock/save the two accounts in a fixed order (ascending ID)
        // regardless of who is sender/receiver, so two users paying each other
        // concurrently can't deadlock on reversed lock order.
        Account first = senderAccount.getId() < receiverAccount.getId() ? senderAccount : receiverAccount;
        Account second = senderAccount.getId() < receiverAccount.getId() ? receiverAccount : senderAccount;

        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(amount));

        try {
            accountRepository.saveAndFlush(first);
            accountRepository.saveAndFlush(second);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account was modified concurrently, please retry", e);
        }

        Transaction transaction = new Transaction(senderId, receiver.getId(), amount, Status.SUCCESS, note);
        return transactionRepository.save(transaction);
    }

    public Page<Transaction> getHistory(Long userId, String window, int page) {
        Instant since = resolveWindowStart(window);
        return transactionRepository.findHistoryForUser(userId, since, PageRequest.of(page, PAGE_SIZE));
    }

    public Transaction getById(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + transactionId));
        boolean isParty = transaction.getSenderId().equals(userId) || transaction.getReceiverId().equals(userId);
        if (!isParty) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a party to this transaction");
        }
        return transaction;
    }

    private Instant resolveWindowStart(String window) {
        String effectiveWindow = StringUtils.hasText(window) ? window : "1W";
        return switch (effectiveWindow) {
            case "1D" -> Instant.now().minus(1, ChronoUnit.DAYS);
            case "1W" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "1M" -> ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid window: " + window + " (expected 1D, 1W, or 1M)");
        };
    }
}
