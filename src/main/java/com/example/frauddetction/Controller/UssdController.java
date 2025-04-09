package com.example.frauddetction.Controller;

import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.service.CustomUssdUserService;
import com.example.frauddetction.model.UssdUserAccount;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/ussd")
public class UssdController {
    @Autowired
    private CustomUssdUserService ussdUserService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String MAIN_MENU = "CON Welcome to Mobile Money\n" +
            "1. Send Money\n" +
            "2. Check Balance\n" +
            "3. Buy Airtime\n" +
            "4. Exit";

    private static final String SEND_MONEY_PROMPT = "CON Enter recipient's phone number:";
    private static final String AMOUNT_PROMPT = "CON Enter amount to send:";
    private static final String AIRTIME_AMOUNT_PROMPT = "CON Enter amount of airtime to buy:";
    private static final String INVALID_INPUT = "END Invalid input. Please try again.";
    private static final String EXIT_MESSAGE = "END Thank you for using our service.";
    private static final String BLACKLISTED_MESSAGE = "END Sorry, your account has been blacklisted. Please contact support.";

    @PostMapping("/process")
    public String processUssdRequest(
            @RequestParam String sessionId,
            @RequestParam String serviceCode,
            @RequestParam String phoneNumber,
            @RequestParam String text) {

        // Check if user is blacklisted
        UseraccountEntity user = userAccountRepository.findByPhoneNumber(phoneNumber);
        if (user != null && "BLACKLISTED".equals(user.getAccountStatus())) {
            return BLACKLISTED_MESSAGE;
        }

        if (text.isEmpty()) {
            return MAIN_MENU;
        }

        String[] levels = text.split("\\*");
        String lastInput = levels[levels.length - 1];

        if (levels.length == 1) {
            switch (lastInput) {
                case "1":
                    return SEND_MONEY_PROMPT;
                case "2":
                    UssdUserAccount account = ussdUserService.getUserAccount(phoneNumber);
                    if (account != null) {
                        return "END Your balance is: " + account.getBalance();
                    }
                    return "END Account not found.";
                case "3":
                    return AIRTIME_AMOUNT_PROMPT;
                case "4":
                    return EXIT_MESSAGE;
                default:
                    return INVALID_INPUT;
            }
        }

        // Handle Send Money flow
        if (levels[0].equals("1")) {
            if (levels.length == 2) {
                String recipientPhone = lastInput;
                
                // Check if recipient is blacklisted
                UseraccountEntity recipient = userAccountRepository.findByPhoneNumber(recipientPhone);
                if (recipient != null && "BLACKLISTED".equals(recipient.getAccountStatus())) {
                    return "END Cannot send money to a blacklisted account.";
                }
                
                UssdUserAccount recipientAccount = ussdUserService.getUserAccount(recipientPhone);
                if (recipientAccount == null) {
                    return "END Recipient not found.";
                }
                return AMOUNT_PROMPT;
            } else if (levels.length == 3) {
                try {
                    double amount = Double.parseDouble(lastInput);
                    String recipientPhone = levels[1];
                    
                    // Get sender's account
                    UssdUserAccount senderAccount = ussdUserService.getUserAccount(phoneNumber);
                    if (senderAccount == null) {
                        return "END Sender account not found.";
                    }

                    // Check if sender has sufficient balance
                    if (senderAccount.getBalance() < amount) {
                        return "END Insufficient balance.";
                    }

                    // Check if recipient is blacklisted
                    UseraccountEntity recipient = userAccountRepository.findByPhoneNumber(recipientPhone);
                    if (recipient != null && "BLACKLISTED".equals(recipient.getAccountStatus())) {
                        return "END Cannot send money to a blacklisted account.";
                    }

                    // Get recipient's account
                    UssdUserAccount recipientAccount = ussdUserService.getUserAccount(recipientPhone);
                    if (recipientAccount == null) {
                        return "END Recipient account not found.";
                    }

                    // Process the transaction
                    boolean success = ussdUserService.transferMoney(phoneNumber, recipientPhone, amount);
                    if (success) {
                        // Create and save transaction record
                        Transaction transaction = new Transaction();
                        transaction.setSenderPhoneNumber(phoneNumber);
                        transaction.setRecipientPhoneNumber(recipientPhone);
                        transaction.setAmount(amount);
                        transaction.setTransactionDate(LocalDateTime.now());
                        transaction.setTransactionType("TRANSFER");
                        transaction.setStatusMessage("SUCCESS");
                        transactionRepository.save(transaction);

                        return "END Transfer successful. Amount: " + amount + " sent to " + recipientPhone;
                    } else {
                        return "END Transfer failed. Please try again later.";
                    }
                } catch (NumberFormatException e) {
                    return INVALID_INPUT;
                }
            }
        }

        // Handle Buy Airtime flow
        if (levels[0].equals("3")) {
            if (levels.length == 2) {
                try {
                    double amount = Double.parseDouble(lastInput);
                    UssdUserAccount account = ussdUserService.getUserAccount(phoneNumber);
                    
                    if (account == null) {
                        return "END Account not found.";
                    }

                    if (account.getBalance() < amount) {
                        return "END Insufficient balance.";
                    }

                    // Process airtime purchase
                    boolean success = ussdUserService.buyAirtime(phoneNumber, amount);
                    if (success) {
                        // Create and save transaction record
                        Transaction transaction = new Transaction();
                        transaction.setSenderPhoneNumber(phoneNumber);
                        transaction.setRecipientPhoneNumber(phoneNumber);
                        transaction.setAmount(amount);
                        transaction.setTransactionDate(LocalDateTime.now());
                        transaction.setTransactionType("AIRTIME");
                        transaction.setStatusMessage("SUCCESS");
                        transactionRepository.save(transaction);

                        return "END Airtime purchase successful. Amount: " + amount;
                    } else {
                        return "END Airtime purchase failed. Please try again later.";
                    }
                } catch (NumberFormatException e) {
                    return INVALID_INPUT;
                }
            }
        }

        return INVALID_INPUT;
    }
}