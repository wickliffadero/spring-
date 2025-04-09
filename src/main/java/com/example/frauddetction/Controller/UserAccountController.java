package com.example.frauddetction.Controller;

import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.UseraccountEntity;
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

@Controller
public class UserAccountController {

    private static final Logger logger = LoggerFactory.getLogger(UserAccountController.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserAccountRepository userAccountRepository;

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

    @PostMapping("/createaccount")
    public String createAccount(@ModelAttribute UseraccountEntity useraccountEntity,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Model model,
                                HttpServletRequest request) {

        logger.debug("POST /createaccount - Received request.");
        logger.debug("POST /createaccount - Username={}, Email={}", useraccountEntity.getUsername(), useraccountEntity.getEmail());
        logger.debug("POST /createaccount - Password={}, Confirm Password={}", useraccountEntity.getPassword(), confirmPassword);
        logger.debug("POST /createaccount - CSRF Token from request parameter: {}", request.getParameter("_csrf"));

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            logger.debug("POST /createaccount - CSRF Token in HttpServletRequest attributes: Parameter Name={}, Token={}", csrfToken.getParameterName(), csrfToken.getToken());
        } else {
            logger.warn("POST /createaccount - CSRF Token NOT found in HttpServletRequest attributes on POST request.");
        }

        // 1. Password Confirmation Check
        if (!useraccountEntity.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            CsrfToken csrfTokenForError = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfTokenForError != null) {
                model.addAttribute("_csrf", csrfTokenForError);
            }
            return "createaccount"; // Return to the form with an error message
        }

        // 2. Check if username already exists
        if (userAccountRepository.findByUsername(useraccountEntity.getUsername()) != null) {
            model.addAttribute("error", "Username already exists.");
            CsrfToken csrfTokenForError = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfTokenForError != null) {
                model.addAttribute("_csrf", csrfTokenForError);
            }
            return "createaccount";
        }


        // Display the form data to the console
        System.out.println("--- Form Data ---");
        System.out.println("Username: " + useraccountEntity.getUsername());
        System.out.println("Email: " + useraccountEntity.getEmail());
        System.out.println("Phone Number (Before Processing): " + useraccountEntity.getPhoneNumber());
        System.out.println("Password: " + useraccountEntity.getPassword());
        System.out.println("Confirm Password (from @RequestParam): " + confirmPassword);
        System.out.println("CSRF Parameter Name (from request): " + request.getParameter("_csrf"));
        System.out.println("------------------");

        // 3. Normalize the phone number to start with '7' and then validate
        String phoneNumber = useraccountEntity.getPhoneNumber();
        if (phoneNumber != null) {
            // Remove any characters that are not digits
            phoneNumber = phoneNumber.replaceAll("[^0-9]", "");

            if (phoneNumber.startsWith("0")) {
                phoneNumber = phoneNumber.substring(1);
            } else if (phoneNumber.startsWith("2547")) {
                phoneNumber = phoneNumber.substring(4);
            } else if (phoneNumber.startsWith("254")) { // Corrected to handle +254
                phoneNumber = phoneNumber.substring(4);
            }

            // --- PHONE NUMBER VALIDATION ---
            if (phoneNumber.length() != 9 || !phoneNumber.matches("[0-9]+")) {
                model.addAttribute("error", "Invalid phone number format. Please enter a 9-digit Kenyan phone number (e.g., 7XXXXXXXX).");
                CsrfToken csrfTokenForError = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrfTokenForError != null) {
                    model.addAttribute("_csrf", csrfTokenForError);
                }
                return "createaccount"; // Return to the form with an error message
            }

            useraccountEntity.setPhoneNumber(phoneNumber);
            System.out.println("Phone Number (After Normalization and Validation): " + useraccountEntity.getPhoneNumber());
        } else {
            System.out.println("Phone Number is null.");
            model.addAttribute("error", "Phone number cannot be empty.");
            CsrfToken csrfTokenForError = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfTokenForError != null) {
                model.addAttribute("_csrf", csrfTokenForError);
            }
            return "createaccount"; // Return to the form with an error message
        }

        // 4. Hash the password before saving
        String hashedPassword = passwordEncoder.encode(useraccountEntity.getPassword());
        useraccountEntity.setPassword(hashedPassword);

        // 5. Save the account to the database
        userAccountRepository.save(useraccountEntity);

        // 6. Redirect or show a success message
        model.addAttribute("message", "Account created successfully!");
        return "login";
    }
}