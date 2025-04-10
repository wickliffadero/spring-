package com.example.frauddetction.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class IPReputationService {
    private final Map<String, IPReputation> ipReputationCache = new HashMap<>();

    public boolean isIPReputable(String ipAddress) {
        IPReputation reputation = ipReputationCache.get(ipAddress);
        if (reputation == null) {
            // In a real system, you would call an external IP reputation service
            reputation = new IPReputation(ipAddress);
            ipReputationCache.put(ipAddress, reputation);
        }
        return reputation.isReputable();
    }

    public void updateIPReputation(String ipAddress, boolean isReputable) {
        IPReputation reputation = ipReputationCache.get(ipAddress);
        if (reputation == null) {
            reputation = new IPReputation(ipAddress);
        }
        reputation.setReputable(isReputable);
        ipReputationCache.put(ipAddress, reputation);
    }

    private static class IPReputation {
        private final String ipAddress;
        private boolean isReputable;
        private int score;

        public IPReputation(String ipAddress) {
            this.ipAddress = ipAddress;
            this.isReputable = true; // Default to reputable
            this.score = 100; // Default score
        }

        public boolean isReputable() {
            return isReputable && score >= 50;
        }

        public void setReputable(boolean reputable) {
            isReputable = reputable;
        }

        public void updateScore(int delta) {
            score = Math.max(0, Math.min(100, score + delta));
        }
    }
} 