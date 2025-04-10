package com.example.frauddetction.Controller;

import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.service.CustomUssdUserService;
import com.example.frauddetction.model.UssdUserAccount;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UseraccountEntity;
import com.example.frauddetction.Repository.UssdUserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/ussd")
public class UssdController {
    @Autowired
    private CustomUssdUserService ussdUserService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UssdUserAccountRepository ussdUserAccountRepository;

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
    private static final String NOT_REGISTERED_MESSAGE = "END Phone number not registered for fraud detection. Please contact admin for registration.";
    private static final String INACTIVE_ACCOUNT_MESSAGE = "END Account is inactive. Please contact admin.";

    //  method to convert phone number formats
    private String convertPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        // Remove any non-digit characters
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        
        // Check if the number contains any non-digit characters
        if (!phoneNumber.matches("^[0-9+]*$")) {
            return null;
        }
        
        // Check if the number is in a valid Kenyan format
        if (digits.length() < 9 || digits.length() > 12) {
            return null;
        }
        
        // If it starts with 254, convert to 07 format
        if (digits.startsWith("254")) {
            return "0" + digits.substring(3);
        }
        // If it's already in 07 format, return it is as is
        else if (digits.startsWith("07")) {
            return digits;
        }
        // If it starts with +254, convert to 07 format
        else if (digits.startsWith("254")) {
            return "0" + digits.substring(3);
        }
        // If it's in any other format, try to convert to 07 format
        else {
            // Ensure the number is 10 digits long (07XXXXXXXX)
            if (digits.length() == 9) {
                return "0" + digits;
            } else if (digits.length() == 10 && digits.startsWith("0")) {
                return digits;
            } else {
                return null;
            }
        }
    }

    @GetMapping
    public ResponseEntity<String> testUssd() {
        return ResponseEntity.ok(MAIN_MENU);
    }

    @PostMapping("/callback")
    public ResponseEntity<String> handleUssdCallback(
            @RequestParam String sessionId,
            @RequestParam String serviceCode,
            @RequestParam String phoneNumber,
            @RequestParam String text) {
        Map<String, String> params = new HashMap<>();
        params.put("sessionId", sessionId);
        params.put("serviceCode", serviceCode);
        params.put("phoneNumber", phoneNumber);
        params.put("text", text);
        return handleUssdRequest(params);
    }

    @PostMapping
    public ResponseEntity<String> handleDirectUssd(
            @RequestParam(required = false, defaultValue = "") String text,
            @RequestParam(required = false, defaultValue = "test-session") String sessionId,
            @RequestParam(required = false, defaultValue = "*123#") String serviceCode,
            @RequestParam(required = true) String phoneNumber) {
        Map<String, String> params = new HashMap<>();
        params.put("sessionId", sessionId);
        params.put("serviceCode", serviceCode);
        params.put("phoneNumber", phoneNumber);
        params.put("text", text);
        return handleUssdRequest(params);
    }

    @PostMapping(value = "/ussd", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleUssdRequest(@RequestParam Map<String, String> params) {
        String sessionId = params.get("sessionId");
        String phoneNumber = params.get("phoneNumber");
        String text = params.get("text");

        try {
            // Convert phone number to database format
            String convertedPhoneNumber = convertPhoneNumber(phoneNumber);
            if (convertedPhoneNumber == null) {
                return ResponseEntity.ok("END Invalid phone number format. Please use a valid Kenyan number starting with 07.");
            }

            // Check if the phone number is registered
            Optional<UssdUserAccount> userOpt = ussdUserAccountRepository.findByPhoneNumber(convertedPhoneNumber);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok("END Please register for fraud detection services first. Your number: " + convertedPhoneNumber);
            }

            UssdUserAccount user = userOpt.get();
            String[] parts = text.split("\\*");
            String response;

            switch (parts.length) {
                case 1:
                    response = "CON Welcome to FraudGuard USSD\n" +
                             "1. Check Balance\n" +
                             "2. Send Money\n" +
                             "3.  Exit\n";
                    break;

                case 2:
                    switch (parts[1]) {
                        case "1":
                            response = "END Your balance is $" + user.getBalance();
                            break;
                        case "2":
                            response = "CON Enter recipient's phone number (07XXXXXXXX)";
                            break;
                        case "3":
                            response = "CON Enter your current PIN";
                            break;
                        case "4":
                            response = "END Thank you for using FraudGuard USSD";
                            break;
                        default:
                            response = "END Invalid option selected";
                    }
                    break;

                case 3:
                    if (parts[1].equals("2")) {
                        String recipientNumber = convertPhoneNumber(parts[2]);
                        if (recipientNumber == null) {
                            response = "END Invalid recipient phone number format";
                        } else {
                            Optional<UssdUserAccount> recipientOpt = ussdUserAccountRepository.findByPhoneNumber(recipientNumber);
                            if (recipientOpt.isEmpty()) {
                                response = "END Recipient not found. Please ensure they are registered with FraudGuard";
                            } else {
                                response = "CON Enter amount to send (max $" + user.getBalance() + ")";
                            }
                        }
                    } else if (parts[1].equals("3")) {
                        if (!parts[2].equals(user.getPin())) {
                            response = "END Incorrect PIN. Please try again";
                        } else {
                            response = "CON Enter new PIN (4 digits)";
                        }
                    } else {
                        response = "END Invalid input";
                    }
                    break;

                case 4:
                    if (parts[1].equals("2")) {
                        try {
                            BigDecimal amount = new BigDecimal(parts[3]);
                            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                                response = "END Amount must be greater than 0";
                            } else if (amount.compareTo(user.getBalance()) > 0) {
                                response = "END Insufficient balance. Your balance is $" + user.getBalance();
                            } else {
                                String recipientNumber = convertPhoneNumber(parts[2]);
                                Optional<UssdUserAccount> recipientOpt = ussdUserAccountRepository.findByPhoneNumber(recipientNumber);
                                if (recipientOpt.isPresent()) {
                                    UssdUserAccount recipient = recipientOpt.get();
                                    // Update sender's balance
                                    user.setBalance(user.getBalance().subtract(amount));
                                    // Update recipient's balance
                                    recipient.setBalance(recipient.getBalance().add(amount));
                                    // Save both accounts
                                    ussdUserAccountRepository.save(user);
                                    ussdUserAccountRepository.save(recipient);
                                    
                                    // Get recipient's full name
                                    String recipientName = recipient.getFirstName() + " " + recipient.getLastName();
                                    if (recipientName.trim().isEmpty()) {
                                        recipientName = "User"; // Fallback if name is not set
                                    }
                                    
                                    response = "END Money sent successfully!\n" +
                                             "Recipient: " + recipientName + "\n" +
                                             "Amount: $" + amount + "\n" +
                                             "Your new balance: $" + user.getBalance();
                                } else {
                                    response = "END Recipient not found";
                                }
                            }
                        } catch (NumberFormatException e) {
                            response = "END Invalid amount format";
                        }
                    } else if (parts[1].equals("3")) {
                        if (parts[3].length() != 4 || !parts[3].matches("\\d+")) {
                            response = "END PIN must be 4 digits";
                        } else {
                            user.setPin(parts[3]);
                            ussdUserAccountRepository.save(user);
                            response = "END PIN changed successfully";
                        }
                    } else {
                        response = "END Invalid input";
                    }
                    break;

                default:
                    response = "END Invalid input format";
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok("END An error occurred. Please try again later");
        }
    }
}