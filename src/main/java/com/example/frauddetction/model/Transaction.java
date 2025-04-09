package com.example.frauddetction.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderPhoneNumber;
    private String recipientPhoneNumber;
    private double amount;
    private LocalDateTime transactionDate;
    private String transactionType;
    private String transactionStatusMessage; // Added field for status messages

    // Default constructor (required by JPA)
    public Transaction() {
        this.transactionDate = LocalDateTime.now();
    }

    public Transaction(String senderPhoneNumber, String recipientPhoneNumber, double amount, String transactionType) {
        this.senderPhoneNumber = senderPhoneNumber;
        this.recipientPhoneNumber = recipientPhoneNumber;
        this.amount = amount;
        this.transactionDate = LocalDateTime.now();
        this.transactionType = transactionType;
    }

    // Getters and setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public void setSenderPhoneNumber(String senderPhoneNumber) {
        this.senderPhoneNumber = senderPhoneNumber;
    }

    public String getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public void setRecipientPhoneNumber(String recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionStatusMessage() {
        return transactionStatusMessage;
    }

    public void setTransactionStatusMessage(String transactionStatusMessage) {
        this.transactionStatusMessage = transactionStatusMessage;
    }
}