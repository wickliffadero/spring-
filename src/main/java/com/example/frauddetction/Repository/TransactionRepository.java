package com.example.frauddetction.Repository;

import com.example.frauddetction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderPhoneNumberOrderByTransactionDateDesc(String senderPhoneNumber);
    List<Transaction> findByRecipientPhoneNumberOrderByTransactionDateDesc(String recipientPhoneNumber);
    List<Transaction> findBySenderPhoneNumberOrRecipientPhoneNumberOrderByTransactionDateDesc(String senderPhoneNumber, String recipientPhoneNumber);
    List<Transaction> findBySenderPhoneNumberOrRecipientPhoneNumberAndTransactionTypeContainingOrderByTransactionDateDesc(String senderPhoneNumber, String recipientPhoneNumber, String transactionType);

    // Add this method declaration:
    List<Transaction> findBySenderPhoneNumberAndRecipientPhoneNumber(String senderPhoneNumber, String recipientPhoneNumber);
}