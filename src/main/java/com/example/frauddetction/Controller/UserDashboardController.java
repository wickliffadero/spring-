package com.example.frauddetction.Controller;

import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UseraccountEntity;
import com.example.frauddetction.util.PhoneNumberFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class UserDashboardController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, RedirectAttributes redirectAttributes) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UseraccountEntity user = userAccountRepository.findByUsername(auth.getName());

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found. Please log in again.");
            return "redirect:/login";
        }

        // Check account status
        if ("BLOCKED".equals(user.getAccountStatus())) {
            redirectAttributes.addFlashAttribute("error", "Your account has been blocked. Please contact support.");
            return "redirect:/login";
        }

        String displayedPhoneNumber = PhoneNumberFormatter.formatPhoneNumberForDisplay(user.getPhoneNumber());
        String comparisonPhoneNumber = PhoneNumberFormatter.formatPhoneNumberForComparison(user.getPhoneNumber());
        
        // Get user's transactions (sent or received)
        List<Transaction> allTransactions = transactionRepository.findBySenderPhoneNumberOrRecipientPhoneNumberOrderByTransactionDateDesc(
            comparisonPhoneNumber, comparisonPhoneNumber);

        // Get flagged transactions for this user
        List<Transaction> flaggedTransactions = transactionRepository.findBySenderPhoneNumberOrRecipientPhoneNumberAndTransactionTypeContainingOrderByTransactionDateDesc(
            comparisonPhoneNumber, comparisonPhoneNumber, "FLAGGED");

        // Calculate transaction statistics
        double totalSent = allTransactions.stream()
                .filter(t -> PhoneNumberFormatter.formatPhoneNumberForComparison(t.getSenderPhoneNumber()).equals(comparisonPhoneNumber))
                .mapToDouble(Transaction::getAmount)
                .sum();
        double totalReceived = allTransactions.stream()
                .filter(t -> PhoneNumberFormatter.formatPhoneNumberForComparison(t.getRecipientPhoneNumber()).equals(comparisonPhoneNumber))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Get recent transactions (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<Transaction> recentTransactions = allTransactions.stream()
                .filter(t -> t.getTransactionDate().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());

        // Calculate daily transaction amounts for the last 7 days
        Map<LocalDateTime, Double> dailySent = recentTransactions.stream()
                .filter(t -> PhoneNumberFormatter.formatPhoneNumberForComparison(t.getSenderPhoneNumber()).equals(comparisonPhoneNumber))
                .collect(Collectors.groupingBy(
                    t -> t.getTransactionDate().truncatedTo(ChronoUnit.DAYS),
                    Collectors.summingDouble(Transaction::getAmount)
                ));

        Map<LocalDateTime, Double> dailyReceived = recentTransactions.stream()
                .filter(t -> PhoneNumberFormatter.formatPhoneNumberForComparison(t.getRecipientPhoneNumber()).equals(comparisonPhoneNumber))
                .collect(Collectors.groupingBy(
                    t -> t.getTransactionDate().truncatedTo(ChronoUnit.DAYS),
                    Collectors.summingDouble(Transaction::getAmount)
                ));

        // Add data to model
        model.addAttribute("phoneNumber", displayedPhoneNumber);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("accountStatus", user.getAccountStatus());
        model.addAttribute("transactions", allTransactions);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("flaggedTransactions", flaggedTransactions);
        model.addAttribute("totalSent", totalSent);
        model.addAttribute("totalReceived", totalReceived);
        model.addAttribute("transactionCount", allTransactions.size());
        model.addAttribute("dailySent", dailySent);
        model.addAttribute("dailyReceived", dailyReceived);
        model.addAttribute("lastLogin", user.getLastLogin());
        
        // Add transaction statistics
        model.addAttribute("totalTransactions", allTransactions.size());
        model.addAttribute("successfulTransactions", allTransactions.stream()
                .filter(t -> !t.getTransactionType().contains("FLAGGED"))
                .count());

        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userAccountRepository.save(user);

        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model, RedirectAttributes redirectAttributes) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UseraccountEntity user = userAccountRepository.findByUsername(auth.getName());

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found. Please log in again.");
            return "redirect:/login";
        }

        // Check account status
        if ("BLOCKED".equals(user.getAccountStatus())) {
            redirectAttributes.addFlashAttribute("error", "Your account has been blocked. Please contact support.");
            return "redirect:/login";
        }

        String displayedPhoneNumber = PhoneNumberFormatter.formatPhoneNumberForDisplay(user.getPhoneNumber());
        
        // Add user data to model
        model.addAttribute("phoneNumber", displayedPhoneNumber);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("accountStatus", user.getAccountStatus());
        model.addAttribute("lastLogin", user.getLastLogin());

        return "profile";
    }

    @GetMapping("/dashboard/payment-history")
    public String paymentHistory(Model model, RedirectAttributes redirectAttributes) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UseraccountEntity user = userAccountRepository.findByUsername(auth.getName());

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found. Please log in again.");
            return "redirect:/login";
        }

        // Check account status
        if ("BLOCKED".equals(user.getAccountStatus())) {
            redirectAttributes.addFlashAttribute("error", "Your account has been blocked. Please contact support.");
            return "redirect:/login";
        }

        // Get all transactions for the user
        List<Transaction> transactions = transactionRepository.findBySenderPhoneNumberOrRecipientPhoneNumberOrderByTransactionDateDesc(
            user.getPhoneNumber(), user.getPhoneNumber());

        // Add data to model
        model.addAttribute("transactions", transactions);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("phoneNumber", PhoneNumberFormatter.formatPhoneNumberForDisplay(user.getPhoneNumber()));

        return "payment-history";
    }

    @GetMapping("/dashboard/download-report")
    public void downloadReport(HttpServletResponse response, RedirectAttributes redirectAttributes) throws IOException {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UseraccountEntity user = userAccountRepository.findByUsername(auth.getName());

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found. Please log in again.");
            return;
        }

        // Check account status
        if ("BLOCKED".equals(user.getAccountStatus())) {
            redirectAttributes.addFlashAttribute("error", "Your account has been blocked. Please contact support.");
            return;
        }

        // Get all transactions for the user
        List<Transaction> transactions = transactionRepository.findBySenderPhoneNumberOrRecipientPhoneNumberOrderByTransactionDateDesc(
            user.getPhoneNumber(), user.getPhoneNumber());

        // Set response headers
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transaction_report.csv");

        // Create CSV writer
        try (PrintWriter writer = response.getWriter()) {
            // Write CSV header
            writer.println("Date,Type,Amount,Status,Recipient/Sender");

            // Write transaction data
            for (Transaction transaction : transactions) {
                String type = transaction.getSenderPhoneNumber().equals(user.getPhoneNumber()) ? "SENT" : "RECEIVED";
                String otherParty = type.equals("SENT") ? transaction.getRecipientPhoneNumber() : transaction.getSenderPhoneNumber();
                String status = transaction.getTransactionType().contains("FLAGGED") ? "FLAGGED" : "COMPLETED";
                
                writer.println(String.format("%s,%s,%.2f,%s,%s",
                    transaction.getTransactionDate(),
                    type,
                    transaction.getAmount(),
                    status,
                    otherParty
                ));
            }
        }
    }

    @GetMapping("/dashboard/report-issue")
    public String reportIssue(Model model, RedirectAttributes redirectAttributes) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UseraccountEntity user = userAccountRepository.findByUsername(auth.getName());

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found. Please log in again.");
            return "redirect:/login";
        }

        // Check account status
        if ("BLOCKED".equals(user.getAccountStatus())) {
            redirectAttributes.addFlashAttribute("error", "Your account has been blocked. Please contact support.");
            return "redirect:/login";
        }

        // Add user data to model
        model.addAttribute("username", user.getUsername());
        model.addAttribute("phoneNumber", PhoneNumberFormatter.formatPhoneNumberForDisplay(user.getPhoneNumber()));

        return "report-issue";
    }
}
