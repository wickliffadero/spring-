package com.example.frauddetction.service;

import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.Repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionPatternService {

    @Autowired
    private TransactionRepository transactionRepository;

    public double analyzeTransactionPattern(Transaction transaction) {
        double score = 100.0; // Start with a perfect score

        // 1. Time of day analysis
        score -= analyzeTimeOfDay(transaction);

        // 2. Amount pattern analysis
        score -= analyzeAmountPattern(transaction);

        // 3. Recipient pattern analysis
        score -= analyzeRecipientPattern(transaction);

        // 4. Frequency pattern analysis
        score -= analyzeFrequencyPattern(transaction);

        return Math.max(0, Math.min(100, score));
    }

    private double analyzeTimeOfDay(Transaction transaction) {
        int hour = transaction.getTransactionDate().getHour();
        // Transactions between 1 AM and 5 AM are more suspicious
        if (hour >= 1 && hour <= 5) {
            return 20.0;
        }
        return 0.0;
    }

    private double analyzeAmountPattern(Transaction transaction) {
        List<Transaction> recentTransactions = transactionRepository
            .findBySenderPhoneNumberAndTransactionDateAfter(
                transaction.getSenderPhoneNumber(),
                LocalDateTime.now().minus(30, ChronoUnit.DAYS)
            );

        double averageAmount = recentTransactions.stream()
            .mapToDouble(Transaction::getAmount)
            .average()
            .orElse(0.0);

        // If the current transaction is significantly larger than the average
        if (transaction.getAmount() > averageAmount * 2) {
            return 30.0;
        }
        return 0.0;
    }

    private double analyzeRecipientPattern(Transaction transaction) {
        List<Transaction> recentTransactions = transactionRepository
            .findBySenderPhoneNumberAndTransactionDateAfter(
                transaction.getSenderPhoneNumber(),
                LocalDateTime.now().minus(30, ChronoUnit.DAYS)
            );

        // Count transactions to the same recipient
        Map<String, Long> recipientCount = recentTransactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getRecipientPhoneNumber,
                Collectors.counting()
            ));

        // If this is the first transaction to this recipient
        if (!recipientCount.containsKey(transaction.getRecipientPhoneNumber())) {
            return 15.0;
        }
        return 0.0;
    }

    private double analyzeFrequencyPattern(Transaction transaction) {
        List<Transaction> recentTransactions = transactionRepository
            .findBySenderPhoneNumberAndTransactionDateAfter(
                transaction.getSenderPhoneNumber(),
                LocalDateTime.now().minus(1, ChronoUnit.HOURS)
            );

        // If there are more than 5 transactions in the last hour
        if (recentTransactions.size() > 5) {
            return 25.0;
        }
        return 0.0;
    }
} 