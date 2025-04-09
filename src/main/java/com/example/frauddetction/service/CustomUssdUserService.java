package com.example.frauddetction.service;

import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.Repository.UssdUserAccountRepository;
import com.example.frauddetction.model.UssdUserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomUssdUserService {

    @Autowired
    private UssdUserAccountRepository ussdUserAccountRepository;
    public String normalizePhoneNumberForDatabase(String phoneNumber) {
        phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
        if (phoneNumber.startsWith("254")) {
            return phoneNumber.substring(3);
        }
        if (phoneNumber.startsWith("0")) {
            return phoneNumber.substring(1);
        }
        return phoneNumber;
    }
    private boolean isValidRecipientPhoneNumber(String phoneNumber, String sessionPhoneNumber) {
        phoneNumber = phoneNumber.trim();
        return phoneNumber.startsWith("07") && phoneNumber.length() == 10 && !normalizePhoneNumberForDatabase(phoneNumber).equals(normalizePhoneNumberForDatabase(sessionPhoneNumber));
    }

    private double parseAmount(String amount) {
        try {
            return Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Method to print all phone numbers and balances in the database for debugging
    private void printAllUserAccounts() {
        System.out.println("Printing all user accounts in the database:");
        Iterable<UssdUserAccount> allUsers = ussdUserAccountRepository.findAll();
        for (UssdUserAccount user : allUsers) {
            String normalizedPhone = normalizePhoneNumberForDatabase(user.getPhoneNumber());
            System.out.println("Phone Number: " + normalizedPhone + ", Balance: $" + user.getBalance());
        }
    }
}
