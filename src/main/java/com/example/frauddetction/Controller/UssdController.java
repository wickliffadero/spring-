package com.example.frauddetction.Controller;

import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UssdUserAccountRepository;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UssdUserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ussd")
public class UssdController {

    @Autowired
    private UssdUserAccountRepository ussdUserAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final Map<String, Map<String, String>> sessionData = new HashMap<>();

    @PostMapping
    public String handleUssdRequest(@RequestParam Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String serviceCode = body.get("serviceCode");
        String phoneNumber = body.get("phoneNumber");
        String text = body.get("text");
        String lastInput = "";

        StringBuilder response = new StringBuilder();
        String normalizedPhoneNumber = normalizePhoneNumberForDatabase(phoneNumber);

        System.out.println("Session ID: " + sessionId);
        System.out.println("Service Code: " + serviceCode);
        System.out.println("Sender Phone Number (Session): " + phoneNumber);
        System.out.println("Normalized Sender Phone Number (Database): " + normalizedPhoneNumber);
        System.out.println("Full Text Input: " + text);
        System.out.println("Session Data: " + sessionData);

        String[] inputs = text.split("\\*");
        if (inputs.length > 0) {
            lastInput = inputs[inputs.length - 1].trim();
            System.out.println("Last Input: " + lastInput);
        }

        sessionData.computeIfAbsent(sessionId, k -> new HashMap<>());
        Map<String, String> currentSession = sessionData.get(sessionId);

        if (text == null || text.isEmpty()) {
            response.append("welcome\n\n1. Check my balance\n\n2. Send money");
        } else if ("1".equals(lastInput) && !currentSession.containsKey("stage")) { // Check balance only at the main menu
            Optional<UssdUserAccount> ussdUserAccountOpt = ussdUserAccountRepository.findByPhoneNumber(normalizedPhoneNumber);
            if (ussdUserAccountOpt.isPresent()) {
                UssdUserAccount ussdUserAccount = ussdUserAccountOpt.get();
                response.append("END Your Account balance is $").append(ussdUserAccount.getBalance());
            } else {
                response.append("Your phoneNumber. " +phoneNumber+" is not registered!");
            }
            sessionData.remove(sessionId);
        } else if ("2".equals(lastInput) && !currentSession.containsKey("stage")) { // Send money from the main menu
            response.append("CON Enter the recipient's phone number:");
            currentSession.put("stage", "enter_recipient_number");
        } else if (currentSession.containsKey("stage") && "enter_recipient_number".equals(currentSession.get("stage"))) {
            String recipientPhoneNumberInput = lastInput;
            System.out.println("Recipient Phone Number (Input): " + recipientPhoneNumberInput);
            if (!isValidRecipientPhoneNumber(recipientPhoneNumberInput, normalizedPhoneNumber)) {
                response.append("END Invalid phone number. Ensure the phone number starts with 07 and is 10 digits long.");
                Transaction cancellation = new Transaction(normalizedPhoneNumber, recipientPhoneNumberInput, 0.0, "SEND_CANCELLED_INVALID_RECIPIENT");
                transactionRepository.save(cancellation);
                sessionData.remove(sessionId);
            } else {
                String normalizedRecipientPhoneNumberForLookup = normalizePhoneNumberForDatabase(recipientPhoneNumberInput);
                currentSession.put("recipientPhoneNumber", normalizedRecipientPhoneNumberForLookup);

                // Check if this is the first time sending to this recipient
                List<Transaction> previousTransactions = transactionRepository.findBySenderPhoneNumberAndRecipientPhoneNumber(normalizedPhoneNumber, normalizedRecipientPhoneNumberForLookup);
                boolean isFirstTime = previousTransactions.isEmpty();
                currentSession.put("firstTimeRecipient", String.valueOf(isFirstTime)); // Store as String

                if (isFirstTime) {
                    response.append("CON This is the first time sending to this number.\n\n1. Continue"); // Prompt to continue
                    currentSession.put("stage", "confirm_first_time_recipient");
                } else {
                    response.append("CON Enter the amount to send:");
                    currentSession.put("stage", "capture_amount");
                }
            }
        } else if (currentSession.containsKey("stage") && "confirm_first_time_recipient".equals(currentSession.get("stage")) && "1".equals(lastInput)) {
            response.append("CON Enter amount:");
            currentSession.put("stage", "capture_amount");
        } else if (currentSession.containsKey("stage") && "capture_amount".equals(currentSession.get("stage"))) {
            String amountText = lastInput.trim();
            try {
                double amountToSend = parseAmount(amountText);
                if (amountToSend <= 0) {
                    response.append("END Amount must be greater than zero. Please enter a valid amount.");
                    Transaction cancellation = new Transaction(normalizedPhoneNumber, currentSession.get("recipientPhoneNumber"), amountToSend, "SEND_CANCELLED_INVALID_AMOUNT");
                    transactionRepository.save(cancellation);
                    sessionData.remove(sessionId);
                } else {
                    String recipientPhoneNumber = currentSession.get("recipientPhoneNumber");
                    currentSession.put("amountToSend", String.valueOf(amountToSend));
                    response.append("CON Confirm send $").append(amountToSend).append(" to ").append(recipientPhoneNumber).append("?\n1. Yes\n2. No");
                    currentSession.put("stage", "confirm_send");
                }
            } catch (NumberFormatException e) {
                response.append("END Invalid amount entered. Please enter a valid number (e.g., 50.00).");
                Transaction cancellation = new Transaction(normalizedPhoneNumber, currentSession.getOrDefault("recipientPhoneNumber", "N/A"), 0.0, "SEND_CANCELLED_AMOUNT_FORMAT_ERROR");
                transactionRepository.save(cancellation);
                sessionData.remove(sessionId);
            }
        } else if (currentSession.containsKey("stage") && "confirm_send".equals(currentSession.get("stage"))) {
            String confirmation = lastInput.trim();
            double amountToSend = Double.parseDouble(currentSession.get("amountToSend"));
            String recipientPhoneNumber = currentSession.get("recipientPhoneNumber");

            if ("1".equals(confirmation)) {
                Optional<UssdUserAccount> senderAccountOpt = ussdUserAccountRepository.findByPhoneNumber(normalizedPhoneNumber);

                if (senderAccountOpt.isPresent()) {
                    UssdUserAccount senderAccount = senderAccountOpt.get();
                    if (senderAccount.getBalance() < amountToSend) {
                        response.append("END Insufficient balance. Transaction failed.");
                        Transaction failedTransaction = new Transaction(normalizedPhoneNumber, recipientPhoneNumber, amountToSend, "SEND_FAILED_INSUFFICIENT_BALANCE");
                        transactionRepository.save(failedTransaction);
                    } else {
                        Optional<UssdUserAccount> recipientAccountOpt = ussdUserAccountRepository.findByPhoneNumber(recipientPhoneNumber);
                        if (recipientAccountOpt.isPresent()) {
                            UssdUserAccount recipientAccount = recipientAccountOpt.get();

                            senderAccount.setBalance(senderAccount.getBalance() - amountToSend);
                            recipientAccount.setBalance(recipientAccount.getBalance() + amountToSend);

                            ussdUserAccountRepository.save(senderAccount);
                            ussdUserAccountRepository.save(recipientAccount);

                            Transaction senderTransaction = new Transaction(normalizedPhoneNumber, recipientPhoneNumber, amountToSend, "SEND_SUCCESSFUL");
                            transactionRepository.save(senderTransaction);

                            Transaction recipientTransaction = new Transaction(recipientPhoneNumber, normalizedPhoneNumber, amountToSend, "RECEIVE_SUCCESSFUL");
                            transactionRepository.save(recipientTransaction);

                            response.append("END Transaction successful.\n");
                            response.append("You have sent $").append(amountToSend).append(" to ").append(recipientPhoneNumber).append(".\n");
                            response.append("Your new balance is $").append(senderAccount.getBalance());
                        } else {
                            response.append("END Recipient account not found.");
                            Transaction failedTransaction = new Transaction(normalizedPhoneNumber, recipientPhoneNumber, amountToSend, "SEND_FAILED_RECIPIENT_NOT_FOUND");
                            transactionRepository.save(failedTransaction);
                        }
                    }
                } else {
                    response.append("END Sender account not found.");
                    Transaction failedTransaction = new Transaction(normalizedPhoneNumber, recipientPhoneNumber, amountToSend, "SEND_FAILED_SENDER_NOT_FOUND");
                    transactionRepository.save(failedTransaction);
                }
                sessionData.remove(sessionId);
            } else if ("2".equals(confirmation)) {
                response.append("END Transaction cancelled by user.");
                Transaction cancellation = new Transaction(normalizedPhoneNumber, recipientPhoneNumber, amountToSend, "SEND_CANCELLED_BY_USER");
                transactionRepository.save(cancellation);
                sessionData.remove(sessionId);
            } else {
                response.append("CON Invalid option. Confirm send $").append(amountToSend).append(" to ").append(recipientPhoneNumber).append("?\n1. Yes\n2. No");
            }
        } else {
            response.append("END Invalid option. Please try again.");
            sessionData.remove(sessionId);
        }

        printAllUserAccounts();
        return response.toString();
    }

    // Normalize the phone number for database lookup
    private String normalizePhoneNumberForDatabase(String phoneNumber) {
        phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
        if (phoneNumber.startsWith("254")) {
            return phoneNumber.substring(3);
        }
        if (phoneNumber.startsWith("0")) {
            return phoneNumber.substring(1);
        }
        return phoneNumber;
    }

    // Validate if the entered phone number is in the correct format (starts with 07 and is 10 digits)
    private boolean isValidRecipientPhoneNumber(String phoneNumber, String sessionPhoneNumber) {
        phoneNumber = phoneNumber.trim();
        return phoneNumber.startsWith("07") && phoneNumber.length() == 10 && !normalizePhoneNumberForDatabase(phoneNumber).equals(normalizePhoneNumberForDatabase(sessionPhoneNumber));
    }

    // Utility method to parse the amount safely
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