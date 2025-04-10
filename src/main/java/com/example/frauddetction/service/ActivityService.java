package com.example.frauddetction.service;

import com.example.frauddetction.Entity.Activity;
import com.example.frauddetction.model.UseraccountEntity;
import com.example.frauddetction.Repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityService {
    @Autowired
    private ActivityRepository activityRepository;

    public void logActivity(String activityType, String description, UseraccountEntity user) {
        Activity activity = new Activity();
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setUser(user);
        activity.setTimestamp(LocalDateTime.now());
        activityRepository.save(activity);
    }

    public void logUserRegistration(UseraccountEntity user) {
        logActivity("REGISTRATION", "New user registration: " + user.getUsername(), user);
    }

    public void logUserBlacklist(UseraccountEntity user) {
        logActivity("BLACKLIST", "User blacklisted: " + user.getUsername(), user);
    }

    public void logUserWhitelist(UseraccountEntity user) {
        logActivity("WHITELIST", "User whitelisted: " + user.getUsername(), user);
    }

    public void logUserActivation(UseraccountEntity user) {
        logActivity("ACTIVATION", "User activated: " + user.getUsername(), user);
    }
} 