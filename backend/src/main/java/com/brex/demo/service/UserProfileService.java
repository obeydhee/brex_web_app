package com.brex.demo.service;

import com.brex.demo.model.UserProfile;
import com.brex.demo.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserProfileRepository userProfileRepository, AccountService accountService,
            PasswordEncoder passwordEncoder) {
        this.userProfileRepository = userProfileRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfile register(String firstName, String lastName, String email, String phoneNumber,
            String paymentName, String rawPassword) {
        if (userProfileRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered: " + email);
        }
        if (userProfileRepository.existsByPaymentName(paymentName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment name is already taken: " + paymentName);
        }
        UserProfile user = new UserProfile(firstName, lastName, email, phoneNumber, paymentName,
                passwordEncoder.encode(rawPassword));
        user = userProfileRepository.save(user);
        accountService.provisionZeroBalanceAccount(user);
        return user;
    }

    public UserProfile findById(Long id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    public UserProfile findByPaymentName(String paymentName) {
        return userProfileRepository.findByPaymentName(paymentName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + paymentName));
    }

    public UserProfile authenticate(String identifier, String rawPassword) {
        UserProfile user = userProfileRepository.findByEmail(identifier)
                .or(() -> userProfileRepository.findByPaymentName(identifier))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return user;
    }
}
