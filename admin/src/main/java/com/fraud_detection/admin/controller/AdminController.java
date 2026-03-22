package com.fraud_detection.admin.controller;

import com.fraud_detection.admin.entity.TransactionLogEntity;
import com.fraud_detection.admin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // GET API to fetch fraud details by status
//    @GetMapping("/transactions/status/{status}")
//    public ResponseEntity<List<TransactionLogEntity>> getByStatus(@PathVariable String status) {
//        List<TransactionLogEntity> transactions = adminService.getByStatus(status);
//        return ResponseEntity.ok(transactions);
//    }


    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("AdminController is alive!");
    }
    @GetMapping("/transactions/status1/{status}")
    public ResponseEntity<List<TransactionLogEntity>> getByStatus(@PathVariable("status") String status) {
        List<TransactionLogEntity> transactions = adminService.getByStatus(status.trim().toUpperCase());
        return ResponseEntity.ok(transactions); // Always returns 200, even if list is empty
    }

    // GET API to fetch all transactions
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionLogEntity>> getAllTransactions() {
        List<TransactionLogEntity> transactions = adminService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    // PUT API to update only the case open status
    @PutMapping("/transactions/{transactionId}/case-open")
    public ResponseEntity<TransactionLogEntity> updateCaseOpen(
            @PathVariable String transactionId,
            @RequestParam String caseOpenStatus) {
        TransactionLogEntity updatedTransaction = adminService.updateCaseOpen(transactionId, caseOpenStatus);
        return ResponseEntity.ok(updatedTransaction);
    }

    // PUT API to update status and case open status together
    @PutMapping("/transactions/{transactionId}/update-status-and-case-open")
    public ResponseEntity<TransactionLogEntity> updateStatusAndCaseOpen(
            @PathVariable String transactionId,
            @RequestParam(required = false) String caseOpenStatus) {
        TransactionLogEntity updatedTransaction = adminService.updateStatusAndCaseOpen(transactionId, caseOpenStatus);
        return ResponseEntity.ok(updatedTransaction);
    }
}