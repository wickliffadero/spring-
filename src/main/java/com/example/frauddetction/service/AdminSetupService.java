package com.example.frauddetction.service;

import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class AdminSetupService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // Check if admin user exists
        if (userAccountRepository.findByUsername("admin") == null) {
            // Create admin user
            UseraccountEntity admin = new UseraccountEntity(
                "admin",
                "admin@frauddetection.com",
                "0000000000", // Default admin phone number
                passwordEncoder.encode("admin123"), // Default admin password
                "ROLE_ADMIN"
            );
            userAccountRepository.save(admin);
            System.out.println("Admin user created successfully!");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
        }
    }
}
