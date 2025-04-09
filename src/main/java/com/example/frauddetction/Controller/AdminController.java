package com.example.frauddetction.Controller;


import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UseraccountEntity;
import com.example.frauddetction.service.AccountStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccountStatusService accountStatusService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get total transactions
        long totalTransactions = transactionRepository.count();
        
        // Get flagged transactions
        List<Transaction> flaggedTransactions = transactionRepository.findByTransactionTypeContaining("FLAGGED");
        
        // Get blocked transactions
        List<Transaction> blockedTransactions = transactionRepository.findByTransactionTypeContaining("BLOCKED");
        
        // Get total users
        long totalUsers = userAccountRepository.count();
        
        // Get recent transactions (limit to 10)
        List<Transaction> recentTransactions = transactionRepository.findTop10ByOrderByTransactionDateDesc();

        // Add attributes to the model
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("flaggedTransactions", flaggedTransactions.size());
        model.addAttribute("blockedTransactions", blockedTransactions.size());
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("recentTransactions", recentTransactions);

        return "admin/dashboard";
    }

    @GetMapping("/transactions")
    public String viewAllTransactions(Model model) {
        List<Transaction> transactions = transactionRepository.findAll();
        model.addAttribute("transactions", transactions);
        return "admin/transactions";
    }

    @GetMapping("/flagged-transactions")
    public String viewFlaggedTransactions(Model model) {
        List<Transaction> flaggedTransactions = transactionRepository.findByTransactionTypeContaining("FLAGGED");
        model.addAttribute("transactions", flaggedTransactions);
        return "admin/flagged-transactions";
    }

    @GetMapping("/users")
    public String viewUsers(Model model) {
        List<UseraccountEntity> users = userAccountRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/whitelist")
    public String viewWhitelistedUsers(Model model) {
        List<UseraccountEntity> whitelistedUsers = userAccountRepository.findByAccountStatus("WHITELISTED");
        model.addAttribute("users", whitelistedUsers);
        return "admin/whitelist";
    }

    @GetMapping("/blacklist")
    public String viewBlacklistedUsers(Model model) {
        List<UseraccountEntity> blacklistedUsers = userAccountRepository.findByAccountStatus("BLACKLISTED");
        model.addAttribute("users", blacklistedUsers);
        return "admin/blacklist";
    }

    @PostMapping("/whitelist")
    public String whitelistUser(@RequestParam String phoneNumber, RedirectAttributes redirectAttributes) {
        UseraccountEntity user = userAccountRepository.findByPhoneNumber(phoneNumber);
        if (user != null) {
            accountStatusService.whitelistUser(user.getId());
            redirectAttributes.addFlashAttribute("success", "User " + user.getUsername() + " has been whitelisted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "User with phone number " + phoneNumber + " not found.");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/blacklist")
    public String blacklistUser(@RequestParam String phoneNumber, RedirectAttributes redirectAttributes) {
        UseraccountEntity user = userAccountRepository.findByPhoneNumber(phoneNumber);
        if (user != null) {
            accountStatusService.blacklistUser(user.getId());
            redirectAttributes.addFlashAttribute("success", "User " + user.getUsername() + " has been blacklisted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "User with phone number " + phoneNumber + " not found.");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/remove-from-whitelist/{userId}")
    public String removeFromWhitelist(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        accountStatusService.activateUser(userId);
        redirectAttributes.addFlashAttribute("success", "User has been removed from whitelist.");
        return "redirect:/admin/whitelist";
    }

    @PostMapping("/remove-from-blacklist/{userId}")
    public String removeFromBlacklist(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        accountStatusService.activateUser(userId);
        redirectAttributes.addFlashAttribute("success", "User has been removed from blacklist.");
        return "redirect:/admin/blacklist";
    }
}
