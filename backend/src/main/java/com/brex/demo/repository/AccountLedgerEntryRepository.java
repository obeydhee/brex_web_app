package com.brex.demo.repository;

import com.brex.demo.model.AccountLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountLedgerEntryRepository extends JpaRepository<AccountLedgerEntry, Long> {

    Page<AccountLedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
}
