package com.example.frauddetction.service;

import com.example.frauddetction.model.UssdUserAccount;
import com.example.frauddetction.Repository.UssdUserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;

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
        BigDecimal amountDecimal = BigDecimal.valueOf(amount);

        if (sender == null || recipient == null || sender.getBalance().compareTo(amountDecimal) < 0) {
            return false;
        }

        sender.setBalance(sender.getBalance().subtract(amountDecimal));
        recipient.setBalance(recipient.getBalance().add(amountDecimal));

        ussdUserAccountRepository.save(sender);
        ussdUserAccountRepository.save(recipient);

        return true;
    }

    public boolean buyAirtime(String phoneNumber, double amount) {
        UssdUserAccount account = getUserAccount(phoneNumber);
        BigDecimal amountDecimal = BigDecimal.valueOf(amount);
        
        if (account == null || account.getBalance().compareTo(amountDecimal) < 0) {
            return false;
        }

        account.setBalance(account.getBalance().subtract(amountDecimal));
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

    public String processUssdRequest(String phoneNumber, String request) {
        Optional<UssdUserAccount> userOpt = ussdUserAccountRepository.findByPhoneNumber(phoneNumber);
        
        if (userOpt.isEmpty()) {
            return "User not found";
        }

        UssdUserAccount user = userOpt.get();
        String[] parts = request.split("\\*");
        
        if (parts.length < 2) {
            return "Invalid request format";
        }

        String action = parts[1].toUpperCase();
        
        switch (action) {
            case "BALANCE":
                return "Your balance is: " + user.getBalance();
                
            case "SEND":
                if (parts.length < 4) {
                    return "Invalid send format. Use: SEND*AMOUNT*PHONE";
                }
                try {
                    BigDecimal amount = new BigDecimal(parts[2]);
                    String recipientPhone = parts[3];
                    
                    // Check if user has sufficient balance
                    if (user.getBalance().compareTo(amount) < 0) {
                        return "Insufficient balance";
                    }
                    
                    // Update sender's balance
                    user.setBalance(user.getBalance().subtract(amount));
                    ussdUserAccountRepository.save(user);
                    
                    // Update recipient's balance
                    Optional<UssdUserAccount> recipientOpt = ussdUserAccountRepository.findByPhoneNumber(recipientPhone);
                    if (recipientOpt.isPresent()) {
                        UssdUserAccount recipient = recipientOpt.get();
                        recipient.setBalance(recipient.getBalance().add(amount));
                        ussdUserAccountRepository.save(recipient);
                        return "Transfer successful. New balance: " + user.getBalance();
                    } else {
                        return "Recipient not found";
                    }
                } catch (NumberFormatException e) {
                    return "Invalid amount";
                }
                
            case "PIN":
                if (parts.length < 3) {
                    return "Invalid PIN format. Use: PIN*NEWPIN";
                }
                String newPin = parts[2];
                if (newPin.length() != 4 || !newPin.matches("\\d+")) {
                    return "PIN must be 4 digits";
                }
                user.setPin(newPin);
                ussdUserAccountRepository.save(user);
                return "PIN updated successfully";
                
            default:
                return "Invalid action. Use: BALANCE, SEND, or PIN";
        }
    }
}
