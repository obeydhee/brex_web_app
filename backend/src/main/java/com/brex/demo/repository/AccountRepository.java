package com.brex.demo.repository;

import com.brex.demo.model.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserProfileId(Long userId);
}
