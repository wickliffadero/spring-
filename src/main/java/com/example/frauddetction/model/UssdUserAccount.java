package com.example.frauddetction.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "ussdUser")
public class UssdUserAccount {

    @Id
    private String phoneNumber; // Phone number
    private double balance; // Account balance

    // Default constructor
    public UssdUserAccount() {}

    // Constructor to initialize phone number and balance
    public UssdUserAccount(String phoneNumber, double balance) {
        this.phoneNumber = phoneNumber;
        this.balance = balance;
    }

    // Getters and Setters
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
