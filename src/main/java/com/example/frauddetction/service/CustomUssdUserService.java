package com.example.frauddetction.service;

import com.example.frauddetction.model.UssdUserAccount;
import com.example.frauddetction.Repository.UssdUserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomUssdUserService {
    @Autowired
    private UssdUserAccountRepository ussdUserAccountRepository;

    public UssdUserAccount getUserAccount(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumberForDatabase(phoneNumber);
        return ussdUserAccountRepository.findByPhoneNumber(normalizedPhone).orElse(null);
    }

    public boolean transferMoney(String senderPhone, String recipientPhone, double amount) {
        UssdUserAccount sender = getUserAccount(senderPhone);
        UssdUserAccount recipient = getUserAccount(recipientPhone);

        if (sender == null || recipient == null || sender.getBalance() < amount) {
            return false;
        }

        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        ussdUserAccountRepository.save(sender);
        ussdUserAccountRepository.save(recipient);

        return true;
    }

    public boolean buyAirtime(String phoneNumber, double amount) {
        UssdUserAccount account = getUserAccount(phoneNumber);
        if (account == null || account.getBalance() < amount) {
            return false;
        }

        account.setBalance(account.getBalance() - amount);
        ussdUserAccountRepository.save(account);
        return true;
    }

    private String normalizePhoneNumberForDatabase(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        // Remove any non-digit characters
        String digitsOnly = phoneNumber.replaceAll("\\D", "");

        // If the number starts with '0', remove it
        if (digitsOnly.startsWith("0")) {
            digitsOnly = digitsOnly.substring(1);
        }

        // If the number starts with '254', remove it
        if (digitsOnly.startsWith("254")) {
            digitsOnly = digitsOnly.substring(3);
        }

        return digitsOnly;
    }
}
