package com.fraud_detection.Fraud_Management.controller;



import com.fraud_detection.Fraud_Management.entity.BlacklistedAccount;
import com.fraud_detection.Fraud_Management.Service.BlacklistService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
// @CrossOrigin // important for frontend
public class BlacklistedAccountController {

    private final BlacklistService blacklistService;

    public BlacklistedAccountController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @GetMapping("/blocked-accounts")
    public List<BlacklistedAccount> getAllBlockedAccounts() {
        return blacklistService.getAllBlockedAccounts();
    }
}
