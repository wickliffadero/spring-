package com.example.frauddetction.Controller;


import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserLoginController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @GetMapping("/")
    public String home() {
        return "home";
    }
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private String formatPhoneNumberForDisplay(String phoneNumber) {
        if (phoneNumber != null && phoneNumber.startsWith("7")) {
            return "0" + phoneNumber;
        }
        return phoneNumber;
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        // Retrieve the UseraccountEntity to access the phoneNumber
        UseraccountEntity loggedInUser = userAccountRepository.findByUsername(username);
        String storedPhoneNumber = loggedInUser.getPhoneNumber();
        String displayedPhoneNumber = formatPhoneNumberForDisplay(storedPhoneNumber);

        // Fetch all transactions for the user, ordered by most recent
        List<Transaction> allTransactions = transactionRepository.findBySenderPhoneNumberOrRecipientPhoneNumberOrderByTransactionDateDesc(storedPhoneNumber, storedPhoneNumber);

        // Fetch flagged transactions for the user, ordered by most recent
        List<Transaction> flaggedTransactions = transactionRepository.findBySenderPhoneNumberOrRecipientPhoneNumberAndTransactionTypeContainingOrderByTransactionDateDesc(storedPhoneNumber, storedPhoneNumber, "FLAGGED");

        model.addAttribute("transactions", allTransactions);
        model.addAttribute("flaggedTransactions", flaggedTransactions);
        model.addAttribute("username", username);
        model.addAttribute("phoneNumber", displayedPhoneNumber); // Use the formatted phone number for display

        return "dashboard";
    }
}