package com.example.frauddetction.Controller;

import com.example.frauddetction.model.Transaction;
import com.example.frauddetction.service.FraudDetectionService;
import com.example.frauddetction.service.IPReputationService;
import com.example.frauddetction.service.TransactionPatternService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Autowired
    private TransactionPatternService transactionPatternService;

    @PostMapping("/process")
    public ResponseEntity<?> processTransaction(@RequestBody Transaction transaction) {
        // 1. Check transaction amount
        if (!fraudDetectionService.checkTransactionAmount(transaction)) {
            transaction.setStatus("FLAGGED");
            transaction.setSuspicious(true);
            return ResponseEntity.badRequest().body("Transaction amount exceeds limit");
        }

        // 2. Check transaction velocity
        if (!fraudDetectionService.checkTransactionVelocity(transaction.getSenderPhoneNumber())) {
            transaction.setStatus("FLAGGED");
            transaction.setSuspicious(true);
            return ResponseEntity.badRequest().body("Too many transactions in a short time");
        }

        // 3. Analyze transaction pattern
        double fraudScore = transactionPatternService.analyzeTransactionPattern(transaction);
        transaction.setFraudScore(fraudScore);

        if (fraudScore < 50) {
            transaction.setStatus("FLAGGED");
            transaction.setSuspicious(true);
            return ResponseEntity.badRequest().body("Suspicious transaction pattern");
        }

        // 7. If all checks pass, process the transaction
        transaction.setStatus("COMPLETED");
        transaction.setSuspicious(false);
        return ResponseEntity.ok(transaction);
    }
} 