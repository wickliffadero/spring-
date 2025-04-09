package com.example.frauddetction.service;

import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountStatusService accountStatusService;

    @Transactional
    public Transaction processTransaction(Transaction transaction) {
        // Check if either sender or recipient is blacklisted
        if (accountStatusService.isPhoneNumberBlacklisted(transaction.getSenderPhoneNumber())) {
            transaction.setTransactionType("BLOCKED");
            transaction.setTransactionStatusMessage("Transaction blocked: Sender account is blacklisted");
            return transactionRepository.save(transaction);
        }

        if (accountStatusService.isPhoneNumberBlacklisted(transaction.getRecipientPhoneNumber())) {
            transaction.setTransactionType("BLOCKED");
            transaction.setTransactionStatusMessage("Transaction blocked: Recipient account is blacklisted");
            return transactionRepository.save(transaction);
        }

        // If amount is suspiciously high and user is not whitelisted, flag the transaction
        if (transaction.getAmount() > 10000 && 
            !accountStatusService.isPhoneNumberWhitelisted(transaction.getSenderPhoneNumber())) {
            transaction.setTransactionType("FLAGGED");
            transaction.setTransactionStatusMessage("Transaction flagged: Amount exceeds safe limit");
            return transactionRepository.save(transaction);
        }

        // Process normal transaction
        transaction.setTransactionType("SUCCESSFUL");
        return transactionRepository.save(transaction);
    }
}
