package com.brex.demo.repository;

import com.brex.demo.model.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByPaymentName(String paymentName);

    boolean existsByEmail(String email);

    boolean existsByPaymentName(String paymentName);
}
