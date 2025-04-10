package com.example.frauddetction.util;

public class PhoneNumberFormatter {
    
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        // Handle different formats
        if (digitsOnly.startsWith("2547")) {
            // Convert +2547... to 7...
            return digitsOnly.substring(3);
        } else if (digitsOnly.startsWith("07")) {
            // Convert 07... to 7...
            return digitsOnly.substring(1);
        } else if (digitsOnly.startsWith("7")) {
            // Already in correct format
            return digitsOnly;
        } else {
            // If it doesn't match any known format, return as is
            return digitsOnly;
        }
    }

    public static String formatPhoneNumberForDisplay(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        // Format for display (always show with leading 0)
        if (digitsOnly.startsWith("7")) {
            return "0" + digitsOnly;
        } else if (digitsOnly.startsWith("2547")) {
            return "0" + digitsOnly.substring(3);
        }
        return digitsOnly;
    }

    public static String formatPhoneNumberForComparison(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        // Convert to comparison format (always starts with 7)
        if (digitsOnly.startsWith("2547")) {
            return digitsOnly.substring(3);
        } else if (digitsOnly.startsWith("07")) {
            return digitsOnly.substring(1);
        } else if (digitsOnly.startsWith("7")) {
            return digitsOnly;
        }
        return digitsOnly;
    }
} 