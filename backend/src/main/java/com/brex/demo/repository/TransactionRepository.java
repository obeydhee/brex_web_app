package com.brex.demo.repository;

import com.brex.demo.model.Transaction;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // NOTE: intentionally explicit JPQL rather than a derived method name like
    // findBySenderIdOrReceiverIdAndCreatedAtGreaterThanEqual — Spring Data binds
    // "And" tighter than "Or" in derived names, which would silently produce
    // "senderId = ?1 OR (receiverId = ?2 AND createdAt >= ?3)" instead of the
    // intended "(senderId = ?1 OR receiverId = ?1) AND createdAt >= ?2".
    @Query("SELECT t FROM Transaction t "
            + "WHERE (t.senderId = :userId OR t.receiverId = :userId) "
            + "AND t.createdAt >= :since "
            + "ORDER BY t.createdAt DESC")
    Page<Transaction> findHistoryForUser(@Param("userId") Long userId, @Param("since") Instant since, Pageable pageable);
}
