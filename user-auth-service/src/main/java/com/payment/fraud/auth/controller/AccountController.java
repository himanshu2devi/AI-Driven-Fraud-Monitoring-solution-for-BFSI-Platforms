package com.payment.fraud.auth.controller;

import com.payment.fraud.auth.entity.Account;
import com.payment.fraud.auth.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{userId}")
    public ResponseEntity<Account> getAccountByUserId(@PathVariable Long userId) {

        System.out.println("user Id :" + userId);
        Account account = accountService.getAccountDetails(userId);
        return ResponseEntity.ok(account);
    }
}
