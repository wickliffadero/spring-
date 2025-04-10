package com.example.frauddetction.Repository;

import com.example.frauddetction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderPhoneNumberOrderByTransactionDateDesc(String senderPhoneNumber);
    List<Transaction> findByRecipientPhoneNumberOrderByTransactionDateDesc(String recipientPhoneNumber);
    List<Transaction> findBySenderPhoneNumberOrRecipientPhoneNumberOrderByTransactionDateDesc(String senderPhoneNumber, String recipientPhoneNumber);
    List<Transaction> findBySenderPhoneNumberOrRecipientPhoneNumberAndTransactionTypeContainingOrderByTransactionDateDesc(String senderPhoneNumber, String recipientPhoneNumber, String transactionType);
    List<Transaction> findBySenderPhoneNumberAndRecipientPhoneNumber(String senderPhoneNumber, String recipientPhoneNumber);
    List<Transaction> findByTransactionTypeContaining(String type);
    List<Transaction> findTop10ByOrderByTransactionDateDesc();

    List<Transaction> findBySenderPhoneNumberAndTransactionDateAfter(String phoneNumber, LocalDateTime oneHourAgo);
}