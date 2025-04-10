package com.example.frauddetction.service;

import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UseraccountEntity makeUserAdmin(String username) {
        UseraccountEntity user = userAccountRepository.findByUsername(username);
        if (user != null) {
            user.setRole("ADMIN");
            return userAccountRepository.save(user);
        }
        return null;
    }

    public UseraccountEntity createAdminUser(String username, String password, String email, String phoneNumber) {
        // Check if user already exists
        if (userAccountRepository.findByUsername(username) != null) {
            return null;
        }

        UseraccountEntity adminUser = new UseraccountEntity();
        adminUser.setUsername(username);
        adminUser.setPassword(passwordEncoder.encode(password));
        adminUser.setEmail(email);
        adminUser.setPhoneNumber(phoneNumber);
        adminUser.setRole("ADMIN");
        
        return userAccountRepository.save(adminUser);
    }

    public void createUser(String username, String password, String role) {
    }

    public void deleteUser(String username) {
    }
}
