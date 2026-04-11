package com.fraud_detection.Fraud_Management.controller;



import com.fraud_detection.Fraud_Management.DTO.FraudSummaryDTO;
import com.fraud_detection.Fraud_Management.Service.FraudSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud-summary")
public class FraudSummaryController {

    private final FraudSummaryService fraudSummaryService;

    public FraudSummaryController(FraudSummaryService fraudSummaryService) {
        this.fraudSummaryService = fraudSummaryService;
    }

    @GetMapping
    public FraudSummaryDTO getFraudSummary() {
        return fraudSummaryService.getFraudSummary();
    }
}
