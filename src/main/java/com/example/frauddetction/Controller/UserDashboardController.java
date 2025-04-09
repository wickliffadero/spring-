package com.example.frauddetction.Controller;

import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserDashboardController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private String formatPhoneNumberForDisplay(String phoneNumber) {
        if (phoneNumber != null && phoneNumber.startsWith("7")) {
            return "0" + phoneNumber;
        }
        return phoneNumber;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UseraccountEntity user = userAccountRepository.findByUsername(auth.getName());

        if (user != null) {
            String displayedPhoneNumber = formatPhoneNumberForDisplay(user.getPhoneNumber());
            
            // Get user's transactions (sent or received)
            List<Transaction> allTransactions = transactionRepository.findBySenderPhoneNumberOrRecipientPhoneNumberOrderByTransactionDateDesc(
                user.getPhoneNumber(), user.getPhoneNumber());

            // Get flagged transactions for this user
            List<Transaction> flaggedTransactions = transactionRepository.findBySenderPhoneNumberOrRecipientPhoneNumberAndTransactionTypeContainingOrderByTransactionDateDesc(
                user.getPhoneNumber(), user.getPhoneNumber(), "FLAGGED");

            // Calculate transaction statistics
            double totalSent = allTransactions.stream()
                    .filter(t -> t.getSenderPhoneNumber().equals(user.getPhoneNumber()))
                    .mapToDouble(Transaction::getAmount)
                    .sum();
            double totalReceived = allTransactions.stream()
                    .filter(t -> t.getRecipientPhoneNumber().equals(user.getPhoneNumber()))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            // Add data to model
            model.addAttribute("phoneNumber", displayedPhoneNumber);
            model.addAttribute("username", user.getUsername());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("transactions", allTransactions);
            model.addAttribute("flaggedTransactions", flaggedTransactions);
            model.addAttribute("totalSent", totalSent);
            model.addAttribute("totalReceived", totalReceived);
            model.addAttribute("transactionCount", allTransactions.size());
        }

        return "dashboard";
    }
}
