package com.example.frauddetction.service;

import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountStatusService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Transactional
    public void whitelistUser(Long userId) {
        UseraccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountStatus("WHITELISTED");
        userAccountRepository.save(user);
    }

    @Transactional
    public void blacklistUser(Long userId) {
        UseraccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountStatus("BLACKLISTED");
        userAccountRepository.save(user);
    }

    @Transactional
    public void activateUser(Long userId) {
        UseraccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountStatus("ACTIVE");
        userAccountRepository.save(user);
    }

    public boolean isPhoneNumberBlacklisted(String phoneNumber) {
        UseraccountEntity user = userAccountRepository.findByPhoneNumber(phoneNumber);
        return user != null && "BLACKLISTED".equals(user.getAccountStatus());
    }

    public boolean isPhoneNumberWhitelisted(String phoneNumber) {
        UseraccountEntity user = userAccountRepository.findByPhoneNumber(phoneNumber);
        return user != null && "WHITELISTED".equals(user.getAccountStatus());
    }

    public boolean canUserTransact(String phoneNumber) {
        return !isPhoneNumberBlacklisted(phoneNumber);
    }
}
