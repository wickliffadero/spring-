package com.example.frauddetction.Controller;

import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.UseraccountEntity;
import com.example.frauddetction.service.UserRoleService;
import com.example.frauddetction.util.PhoneNumberFormatter;
import com.example.frauddetction.service.ActivityService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserAccountController {

    private static final Logger logger = LoggerFactory.getLogger(UserAccountController.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private ActivityService activityService;

    @PostConstruct
    public void init() {
        // Create admin user if it doesn't exist
        UseraccountEntity existingAdmin = userAccountRepository.findByUsername("admin");
        if (existingAdmin == null) {
            UseraccountEntity adminUser = new UseraccountEntity();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setEmail("admin@system.com");
            adminUser.setPhoneNumber("0700000000");
            adminUser.setRole("ADMIN");
            adminUser.setAccountStatus("ACTIVE");
            userAccountRepository.save(adminUser);
            logger.info("Admin user created successfully");
        }
    }

    @GetMapping("/createaccount")
    public String showCreateAccountForm(Model model, HttpServletRequest request) {
        model.addAttribute("useraccountEntity", new UseraccountEntity());
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
            logger.debug("GET /createaccount - CSRF Token in Model: Parameter Name={}, Token={}", csrfToken.getParameterName(), csrfToken.getToken());
        } else {
            logger.warn("GET /createaccount - CSRF Token NOT found in HttpServletRequest attributes.");
        }
        return "createaccount";
    }

    @PostMapping("/create")
    public String createAccount(@ModelAttribute("user") UseraccountEntity user,
                              @RequestParam("confirmPassword") String confirmPassword,
                              Model model,
                              HttpServletRequest request) {
        logger.debug("POST /createaccount - Received request.");
        logger.debug("POST /createaccount - Username={}, Email={}", user.getUsername(), user.getEmail());
        logger.debug("POST /createaccount - Password={}, Confirm Password={}", user.getPassword(), confirmPassword);
        logger.debug("POST /createaccount - CSRF Token from request parameter: {}", request.getParameter("_csrf"));

        // 1. Password Confirmation Check
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            CsrfToken csrfTokenForError = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfTokenForError != null) {
                model.addAttribute("_csrf", csrfTokenForError);
            }
            return "createaccount";
        }

        // 2. Check if username already exists
        if (userAccountRepository.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Username already exists.");
            CsrfToken csrfTokenForError = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfTokenForError != null) {
                model.addAttribute("_csrf", csrfTokenForError);
            }
            return "createaccount";
        }

        // Format phone number
        String formattedPhoneNumber = PhoneNumberFormatter.formatPhoneNumber(user.getPhoneNumber());
        if (formattedPhoneNumber == null || formattedPhoneNumber.length() != 10) {
            model.addAttribute("error", "Invalid phone number format. Please enter a valid Kenyan phone number.");
            CsrfToken csrfTokenForError = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfTokenForError != null) {
                model.addAttribute("_csrf", csrfTokenForError);
            }
            return "createaccount";
        }

        // Set formatted phone number
        user.setPhoneNumber(formattedPhoneNumber);

        // 3. Hash the password before saving
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        // 4. Set default role to USER
        user.setRole("USER");
        user.setAccountStatus("ACTIVE");

        // Save the user
        userAccountRepository.save(user);
        
        // Log the activity
        activityService.logUserRegistration(user);
        
        model.addAttribute("success", "Account created successfully!");
        return "redirect:/login";
    }

    @GetMapping("/check-admin")
    public String checkAdmin(Model model) {
        UseraccountEntity adminUser = userAccountRepository.findByUsername("admin");
        if (adminUser != null) {
            model.addAttribute("message", 
                "Admin exists - Username: " + adminUser.getUsername() + 
                ", Role: " + adminUser.getRole() + 
                ", Status: " + adminUser.getAccountStatus());
        } else {
            model.addAttribute("message", "No admin user found");
        }
        return "admin-check";
    }

    @GetMapping("/create-admin")
    public String createAdmin(Model model) {
        // Check if admin exists
        UseraccountEntity existingAdmin = userAccountRepository.findByUsername("admin");
        if (existingAdmin != null) {
            // Update existing admin's role and password
            existingAdmin.setRole("ADMIN");  // Will be prefixed with ROLE_ in getAuthorities()
            existingAdmin.setPassword(passwordEncoder.encode("admin123"));
            userAccountRepository.save(existingAdmin);
            model.addAttribute("message", "Admin account updated successfully");
        } else {
            // Create new admin
            UseraccountEntity adminUser = new UseraccountEntity();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setEmail("admin@system.com");
            adminUser.setPhoneNumber("0700000000");
            adminUser.setRole("ADMIN");  // Will be prefixed with ROLE_ in getAuthorities()
            adminUser.setAccountStatus("ACTIVE");
            userAccountRepository.save(adminUser);
            model.addAttribute("message", "Admin account created successfully");
        }
        return "admin-check";
    }

    @PostMapping("/blacklist")
    public String blacklistUser(@RequestParam("phoneNumber") String phoneNumber) {
        UseraccountEntity user = userAccountRepository.findByPhoneNumber(phoneNumber);
        if (user != null) {
            user.setAccountStatus("BLACKLISTED");
            userAccountRepository.save(user);
            
            // Log the activity
            activityService.logUserBlacklist(user);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/whitelist")
    public String whitelistUser(@RequestParam("phoneNumber") String phoneNumber) {
        UseraccountEntity user = userAccountRepository.findByPhoneNumber(phoneNumber);
        if (user != null) {
            user.setAccountStatus("WHITELISTED");
            userAccountRepository.save(user);
            
            // Log the activity
            activityService.logUserWhitelist(user);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/activate")
    public String activateUser(@RequestParam("phoneNumber") String phoneNumber) {
        UseraccountEntity user = userAccountRepository.findByPhoneNumber(phoneNumber);
        if (user != null) {
            user.setAccountStatus("ACTIVE");
            userAccountRepository.save(user);
            
            // Log the activity
            activityService.logUserActivation(user);
        }
        return "redirect:/admin/users";
    }
}