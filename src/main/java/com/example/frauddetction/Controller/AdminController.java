package com.example.frauddetction.Controller;

import com.example.frauddetction.Repository.TransactionRepository;
import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.Repository.ActivityRepository;
import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.model.UseraccountEntity;
import com.example.frauddetction.service.AccountStatusService;
import com.example.frauddetction.Entity.Activity;
import com.example.frauddetction.model.UssdUserAccount;
import com.example.frauddetction.Repository.UssdUserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccountStatusService accountStatusService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UssdUserAccountRepository ussdUserAccountRepository;

    @Autowired
    private HttpSession session;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
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

        // Add recent activities to the model
        List<Activity> recentActivities = activityRepository.findRecentActivities();

        // Add attributes to the model
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("flaggedTransactions", flaggedTransactions.size());
        model.addAttribute("blockedTransactions", blockedTransactions.size());
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("recentActivities", recentActivities);

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

    @PostMapping("/users/delete/{userId}")
    public String deleteUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        UseraccountEntity user = userAccountRepository.findById(userId).orElse(null);
        if (user != null) {
            // Don't allow deleting the last admin
            if ("ADMIN".equals(user.getRole()) && userAccountRepository.countByRole("ADMIN") <= 1) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete the last admin user");
                return "redirect:/admin/users";
            }
            userAccountRepository.delete(user);
            redirectAttributes.addFlashAttribute("success", "User " + user.getUsername() + " has been deleted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new UseraccountEntity());
        return "admin/add-user";
    }

    @PostMapping("/users/create")
    public String createAccount(@ModelAttribute("user") UseraccountEntity user,
                              @RequestParam("confirmPassword") String confirmPassword,
                              HttpSession session) {
        // Check CSRF token
        String sessionToken = (String) session.getAttribute("csrfToken");
        if (sessionToken == null || !sessionToken.equals(user.getCsrfToken())) {
            return "redirect:/admin/users/add?error=Invalid request";
        }

        // Check if passwords match
        if (!user.getPassword().equals(confirmPassword)) {
            return "redirect:/admin/users/add?error=Passwords do not match";
        }

        // Check if username already exists
        if (userAccountRepository.existsByUsername(user.getUsername())) {
            return "redirect:/admin/users/add?error=Username already exists";
        }

        // Check if phone number already exists
        if (userAccountRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            return "redirect:/admin/users/add?error=Phone number already registered";
        }

        // Validate phone number format
        if (!user.getPhoneNumber().matches("^07[0-9]{8}$")) {
            return "redirect:/admin/users/add?error=Invalid phone number format. Use format: 07XXXXXXXX";
        }

        // Set default values
        user.setAccountStatus("ACTIVE");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setLastUpdated(LocalDateTime.now());

        // Hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save the user
        try {
            userAccountRepository.save(user);
            return "redirect:/admin/users/add?success=User created successfully";
        } catch (Exception e) {
            return "redirect:/admin/users/add?error=Error creating user: " + e.getMessage();
        }
    }

    @GetMapping("/users/edit/{userId}")
    public String showEditUserForm(@PathVariable Long userId, Model model) {
        UseraccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + userId));
        model.addAttribute("user", user);
        return "admin/edit-user";
    }

    @PostMapping("/users/edit/{userId}")
    public String updateUser(@PathVariable Long userId, 
                           @ModelAttribute UseraccountEntity updatedUser,
                           @RequestParam(required = false) String newPassword,
                           @RequestParam(required = false) String confirmPassword,
                           RedirectAttributes redirectAttributes) {
        UseraccountEntity existingUser = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + userId));

        // Update basic information
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setAccountStatus(updatedUser.getAccountStatus());

        // Update password if provided
        if (newPassword != null && !newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/admin/users/edit/" + userId;
            }
            existingUser.setPassword(passwordEncoder.encode(newPassword));
        }

        userAccountRepository.save(existingUser);
        redirectAttributes.addFlashAttribute("success", "User updated successfully");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/search")
    public String searchUsers(@RequestParam String query, Model model) {
        List<UseraccountEntity> users = userAccountRepository.findByUsernameContainingOrPhoneNumberContaining(query, query);
        model.addAttribute("users", users);
        model.addAttribute("searchQuery", query);
        return "admin/users";
    }

    @PostMapping("/users/bulk-delete")
    public String bulkDeleteUsers(@RequestParam List<Long> userIds, RedirectAttributes redirectAttributes) {
        for (Long userId : userIds) {
            UseraccountEntity user = userAccountRepository.findById(userId).orElse(null);
            if (user != null && !("ADMIN".equals(user.getRole()) && userAccountRepository.countByRole("ADMIN") <= 1)) {
                userAccountRepository.delete(user);
            }
        }
        redirectAttributes.addFlashAttribute("success", "Selected users deleted successfully");
        return "redirect:/admin/users";
    }

    @GetMapping("/check-role")
    @ResponseBody
    public ResponseEntity<?> checkAdminRole() {
        UseraccountEntity admin = userAccountRepository.findByUsername("admin");
        if (admin != null) {
            return ResponseEntity.ok(Map.of(
                "username", admin.getUsername(),
                "role", admin.getRole(),
                "status", admin.getAccountStatus()
            ));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/settings")
    public String showSettings() {
        return "admin/settings";
    }

    @PostMapping("/settings/theme")
    public String toggleTheme(HttpSession session) {
        String currentTheme = (String) session.getAttribute("theme");
        if (currentTheme == null || currentTheme.equals("light")) {
            session.setAttribute("theme", "dark");
        } else {
            session.setAttribute("theme", "light");
        }
        return "redirect:/admin/settings";
    }

    @GetMapping("/users/add-ussd")
    public String showAddUssdUserForm(Model model) {
        model.addAttribute("ussdUser", new UssdUserAccount());
        return "admin/add-ussd-user";
    }

    @PostMapping("/users/add-ussd")
    public String addUssdUser(@ModelAttribute("ussdUser") UssdUserAccount ussdUser, 
                            RedirectAttributes redirectAttributes) {
        try {
            // Validate phone number
            String phoneNumber = ussdUser.getPhoneNumber();
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Phone number is required");
                return "redirect:/admin/users/add-ussd";
            }

            // Normalize phone number
            phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
            if (!phoneNumber.matches("^07[0-9]{8}$")) {
                redirectAttributes.addFlashAttribute("error", "Invalid phone number format. Must start with 07 and be 10 digits");
                return "redirect:/admin/users/add-ussd";
            }

            // Check if user already exists
            if (ussdUserAccountRepository.findByPhoneNumber(phoneNumber).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "USSD user with phone number " + phoneNumber + " already exists");
                return "redirect:/admin/users/add-ussd";
            }

            // Validate PIN
            String pin = ussdUser.getPin();
            if (pin == null || pin.length() != 4 || !pin.matches("\\d+")) {
                redirectAttributes.addFlashAttribute("error", "PIN must be exactly 4 digits");
                return "redirect:/admin/users/add-ussd";
            }

            // Set required fields
            ussdUser.setPhoneNumber(phoneNumber);
            ussdUser.setBalance(BigDecimal.ZERO);
            ussdUser.setActive(true);
            ussdUser.setCreatedAt(LocalDateTime.now());
            ussdUser.setLastUpdated(LocalDateTime.now());
            ussdUser.setStatus("ACTIVE");

            // Save the USSD user
            UssdUserAccount savedUser = ussdUserAccountRepository.save(ussdUser);
            
            if (savedUser != null) {
                // Log the activity
                Activity activity = new Activity();
                activity.setActivityType("CREATE_USSD_ACCOUNT");
                activity.setDescription("Created USSD account for " + phoneNumber);
                activity.setTimestamp(LocalDateTime.now());
                activityRepository.save(activity);
                
                redirectAttributes.addFlashAttribute("success", "USSD user added successfully");
                return "redirect:/admin/dashboard";
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to save USSD user");
                return "redirect:/admin/users/add-ussd";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding USSD user: " + e.getMessage());
            return "redirect:/admin/users/add-ussd";
        }
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        // Get total users count
        long totalUsers = userAccountRepository.count();
        
        // Get active users count
        long activeUsers = userAccountRepository.countByAccountStatus("ACTIVE");
        
        // Get blacklisted users count
        long blacklistedUsers = userAccountRepository.countByAccountStatus("BLACKLISTED");
        
        // Get all USSD accounts
        List<UssdUserAccount> ussdAccounts = ussdUserAccountRepository.findAll();
        
        // Get recent transactions (last 10)
        List<Transaction> recentTransactions = transactionRepository.findTop10ByOrderByTransactionDateDesc();
        
        // Add counts and data to model
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("blacklistedUsers", blacklistedUsers);
        model.addAttribute("ussdAccounts", ussdAccounts);
        model.addAttribute("recentTransactions", recentTransactions);
        
        return "admin/dashboard";
    }

    @PostMapping("/ussd/topup")
    public String topUpUssdAccount(
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("amount") BigDecimal amount,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Validate amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than 0");
                return "redirect:/admin/dashboard";
            }

            // Convert phone number to database format
            String convertedPhoneNumber = convertPhoneNumber(phoneNumber);
            if (convertedPhoneNumber == null) {
                redirectAttributes.addFlashAttribute("error", "Invalid phone number format. Please use a valid Kenyan number starting with 07.");
                return "redirect:/admin/dashboard";
            }

            // Find the USSD account
            Optional<UssdUserAccount> accountOpt = ussdUserAccountRepository.findByPhoneNumber(convertedPhoneNumber);
            if (accountOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "USSD account not found");
                return "redirect:/admin/dashboard";
            }

            UssdUserAccount account = accountOpt.get();
            
            // Check if the new balance would exceed the maximum limit
            BigDecimal newBalance = account.getBalance().add(amount);
            if (newBalance.compareTo(new BigDecimal("5000")) > 0) {
                redirectAttributes.addFlashAttribute("error", "Top-up would exceed maximum balance of $5,000");
                return "redirect:/admin/dashboard";
            }

            // Update the balance
            account.setBalance(newBalance);
            ussdUserAccountRepository.save(account);

            // Log the activity
            Activity activity = new Activity();
            activity.setActivityType("USSD_TOPUP");
            
            // Create a personalized message with the user's name
            String userName = account.getFirstName() + " " + account.getLastName();
            if (userName.trim().isEmpty()) {
                userName = "User"; // Fallback if name is not set
            }
            
            activity.setDescription("Top-up of $" + amount + " to " + userName + "'s account (" + convertedPhoneNumber + ")");
            activity.setTimestamp(LocalDateTime.now());
            
            // Get the admin user from the session
            UseraccountEntity adminUser = (UseraccountEntity) session.getAttribute("adminUser");
            if (adminUser != null) {
                activity.setUser(adminUser);
            }
            
            activityRepository.save(activity);

            // Update success message to include user's name
            redirectAttributes.addFlashAttribute("success", 
                "Successfully topped up $" + amount + " to " + userName + "'s account (" + convertedPhoneNumber + ")");
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to top up account: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    // Helper method to convert phone number formats
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
        // If it's already in 07 format, return as is
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

    @GetMapping("/ussd/users")
    public String listUssdUsers(Model model) {
        try {
            // Fetch all USSD users from the database
            List<UssdUserAccount> ussdAccounts = ussdUserAccountRepository.findAll();
            
            // Add the list to the model
            model.addAttribute("ussdAccounts", ussdAccounts);
            
            // Log the activity
            Activity activity = new Activity();
            activity.setActivityType("VIEW_USSD_USERS");
            activity.setDescription("Viewed list of USSD users");
            activity.setTimestamp(LocalDateTime.now());
            
            // Get the current admin user from the session
            UseraccountEntity adminUser = (UseraccountEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            activity.setUser(adminUser);
            activityRepository.save(activity);
            
            return "admin/ussd-users";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to fetch USSD users: " + e.getMessage());
            return "admin/ussd-users";
        }
    }

    @PostMapping("/ussd/delete/{phoneNumber}")
    public String deleteUssdUser(@PathVariable String phoneNumber, RedirectAttributes redirectAttributes) {
        try {
            // Find the user
            Optional<UssdUserAccount> userOpt = ussdUserAccountRepository.findByPhoneNumber(phoneNumber);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/ussd/users";
            }

            // Delete the user
            ussdUserAccountRepository.delete(userOpt.get());

            // Log the activity
            Activity activity = new Activity();
            activity.setActivityType("DELETE_USSD_USER");
            activity.setDescription("Deleted USSD user: " + phoneNumber);
            activity.setTimestamp(LocalDateTime.now());
            
            // Get the current admin user from the session
            UseraccountEntity adminUser = (UseraccountEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            activity.setUser(adminUser);
            activityRepository.save(activity);

            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/admin/ussd/users";
    }

    @GetMapping("/ussd/edit/{phoneNumber}")
    public String showEditUssdUserForm(@PathVariable String phoneNumber, Model model) {
        try {
            Optional<UssdUserAccount> userOpt = ussdUserAccountRepository.findByPhoneNumber(phoneNumber);
            if (userOpt.isEmpty()) {
                model.addAttribute("error", "User not found");
                return "redirect:/admin/ussd/users";
            }

            model.addAttribute("user", userOpt.get());
            return "admin/edit-ussd-user";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load user: " + e.getMessage());
            return "redirect:/admin/ussd/users";
        }
    }

    @PostMapping("/ussd/edit/{phoneNumber}")
    public String updateUssdUser(@PathVariable String phoneNumber,
                               @ModelAttribute UssdUserAccount updatedUser,
                               RedirectAttributes redirectAttributes) {
        try {
            Optional<UssdUserAccount> existingUserOpt = ussdUserAccountRepository.findByPhoneNumber(phoneNumber);
            if (existingUserOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/ussd/users";
            }

            UssdUserAccount existingUser = existingUserOpt.get();
            
            // Update user details
            existingUser.setFirstName(updatedUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName());
            existingUser.setServiceProvider(updatedUser.getServiceProvider());
            existingUser.setBalance(updatedUser.getBalance());
            existingUser.setActive(updatedUser.isActive());
            
            // Only update PIN if it's provided and not empty
            if (updatedUser.getPin() != null && !updatedUser.getPin().isEmpty()) {
                existingUser.setPin(updatedUser.getPin());
            }

            ussdUserAccountRepository.save(existingUser);

            // Log the activity
            Activity activity = new Activity();
            activity.setActivityType("UPDATE_USSD_USER");
            activity.setDescription("Updated USSD user: " + phoneNumber);
            activity.setTimestamp(LocalDateTime.now());
            
            // Get the current admin user from the session
            UseraccountEntity adminUser = (UseraccountEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            activity.setUser(adminUser);
            activityRepository.save(activity);

            redirectAttributes.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
        }
        return "redirect:/admin/ussd/users";
    }

    @PostMapping("/admin/ussd/verify")
    public String verifyUssdUser(@RequestParam String phoneNumber, RedirectAttributes redirectAttributes) {
        try {
            // Convert phone number to database format
            String convertedPhoneNumber = convertPhoneNumber(phoneNumber);
            if (convertedPhoneNumber == null) {
                redirectAttributes.addFlashAttribute("error", "Invalid phone number format. Please use a valid Kenyan number starting with 07.");
                return "redirect:/admin/ussd/users";
            }

            // Check if user already exists
            Optional<UssdUserAccount> existingUser = ussdUserAccountRepository.findByPhoneNumber(convertedPhoneNumber);
            if (existingUser.isPresent()) {
                redirectAttributes.addFlashAttribute("success", "User already exists with phone number: " + convertedPhoneNumber);
                return "redirect:/admin/ussd/users";
            }

            // Create new USSD user
            UssdUserAccount newUser = new UssdUserAccount();
            newUser.setPhoneNumber(convertedPhoneNumber);
            newUser.setBalance(BigDecimal.ZERO);
            newUser.setActive(true);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setLastUpdated(LocalDateTime.now());
            newUser.setPin("1234"); // Default PIN, should be changed by user
            newUser.setServiceProvider("Safaricom"); // Default provider

            ussdUserAccountRepository.save(newUser);

            // Log the activity
            Activity activity = new Activity();
            activity.setActivityType("CREATE_USSD");
            activity.setDescription("Created USSD account for " + convertedPhoneNumber);
            activity.setTimestamp(LocalDateTime.now());
            UseraccountEntity adminUser = (UseraccountEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            activity.setUser(adminUser);
            activityRepository.save(activity);

            redirectAttributes.addFlashAttribute("success", "USSD user created successfully with phone number: " + convertedPhoneNumber);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create USSD user: " + e.getMessage());
        }
        return "redirect:/admin/ussd/users";
    }
}
