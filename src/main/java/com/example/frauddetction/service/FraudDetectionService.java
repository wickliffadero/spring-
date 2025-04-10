package com.example.frauddetction.service;

import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FraudDetectionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    // Thresholds for fraud detection
    private static final double MAX_TRANSACTION_AMOUNT = 100000.0; // Maximum allowed transaction amount
    private static final int MAX_TRANSACTIONS_PER_HOUR = 10; // Maximum transactions per hour
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 3; // Maximum failed login attempts
    // Store failed login attempts
    private final Map<String, Integer> failedLoginAttempts = new HashMap<>();
    private final Map<String, LocalDateTime> lastLoginAttempt = new HashMap<>();

    public boolean checkTransactionAmount(Transaction transaction) {
        return transaction.getAmount() <= MAX_TRANSACTION_AMOUNT;
    }

    public boolean checkTransactionVelocity(String phoneNumber) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minus(1, ChronoUnit.HOURS);
        List<Transaction> recentTransactions = transactionRepository
            .findBySenderPhoneNumberAndTransactionDateAfter(phoneNumber, oneHourAgo);
        return recentTransactions.size() <= MAX_TRANSACTIONS_PER_HOUR;
    }

    public boolean checkFailedLoginAttempts(String username) {
        Integer attempts = failedLoginAttempts.getOrDefault(username, 0);
        return attempts < MAX_FAILED_LOGIN_ATTEMPTS;
    }

    public void incrementFailedLoginAttempts(String username) {
        failedLoginAttempts.merge(username, 1, Integer::sum);
        lastLoginAttempt.put(username, LocalDateTime.now());
    }

    public void resetFailedLoginAttempts(String username) {
        failedLoginAttempts.remove(username);
        lastLoginAttempt.remove(username);
    }

    public boolean analyzeTransactionPattern(Transaction transaction) {
        // Analyze transaction patterns based on:
        // 1. Time of day
        // 2. Amount patterns
        // 3. Recipient patterns
        // 4. Frequency patterns
        return true; // Placeholder for actual implementation
    }

    public boolean isTransactionSuspicious(Transaction transaction) {
        return !checkTransactionAmount(transaction) ||
               !checkTransactionVelocity(transaction.getSenderPhoneNumber()) ||
               !analyzeTransactionPattern(transaction);
    }
} 