package com.brex.demo;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brex.demo.model.Account;
import com.brex.demo.model.UserProfile;
import com.brex.demo.repository.AccountRepository;
import com.brex.demo.repository.UserProfileRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies the {@code @Version} optimistic-locking mechanism on {@link Account}
 * directly, rather than via real concurrent HTTP requests: SQLite serializes
 * writers at the file level, so genuine multi-threaded races rarely exercise
 * the conflict path deterministically. Simulating two stale reads racing to
 * save proves the mechanism works without depending on real thread timing.
 */
@SpringBootTest
class AccountServiceTest {

    @DynamicPropertySource
    static void sqliteProperties(DynamicPropertyRegistry registry) throws IOException {
        var tempFile = Files.createTempFile("brex-demo-test", ".db");
        tempFile.toFile().deleteOnExit();
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + tempFile);
    }

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void concurrentStaleUpdatesAreRejectedByOptimisticLocking() {
        UserProfile user = userProfileRepository.save(
                new UserProfile("Lock", "Test", "lock-test@example.com", "555-0100", "lock_test", "hash"));
        Long accountId = accountRepository.save(new Account(user)).getId();

        // Two independent reads of the same row, simulating two requests that
        // both loaded the account before either one wrote back.
        Account firstStaleRead = accountRepository.findById(accountId).orElseThrow();
        Account secondStaleRead = accountRepository.findById(accountId).orElseThrow();

        firstStaleRead.setBalance(new BigDecimal("10.00"));
        accountRepository.save(firstStaleRead); // succeeds, bumps the version

        secondStaleRead.setBalance(new BigDecimal("20.00"));
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> accountRepository.save(secondStaleRead)); // still holds the old version
    }
}
