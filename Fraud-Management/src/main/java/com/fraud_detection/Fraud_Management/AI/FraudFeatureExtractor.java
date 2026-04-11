package com.fraud_detection.Fraud_Management.AI;



import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FraudFeatureExtractor {

    public List<Double> extract(TransactionDTO tx) {

        return List.of(
                tx.getAmount(),

                // Dummy logic (you can improve later)
                tx.getAmount() > 10000 ? 1.0 : 0.0,

                // night check (simulate)
                1.0,

                // velocity (simulate)
                3.0,

                // new merchant (simulate)
                1.0,

                // account age (dummy)
                200.0,

                // failed login attempts (dummy)
                1.0
        );
    }
}
