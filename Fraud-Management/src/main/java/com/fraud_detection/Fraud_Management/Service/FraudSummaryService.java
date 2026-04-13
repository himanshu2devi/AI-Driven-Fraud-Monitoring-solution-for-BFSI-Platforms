package com.fraud_detection.Fraud_Management.Service;

import com.fraud_detection.Fraud_Management.DTO.FraudSummaryDTO;
import com.fraud_detection.Fraud_Management.entity.TransactionLog;
import com.fraud_detection.Fraud_Management.repository.TransactionLogRepository;
import com.fraud_detection.Fraud_Management.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FraudSummaryService {




    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private TransactionRepository transactionRepository;


    public FraudSummaryDTO getFraudSummary() {

        FraudSummaryDTO dto = new FraudSummaryDTO();

        // total logs
        long total = transactionLogRepository.count();

        // counts based on fraud engine output
        long fraud = transactionLogRepository.countByStatus("FRAUD");
        long alert = transactionLogRepository.countByStatus("ALERT");
        long valid = transactionLogRepository.countByStatus("VALID");

        long success = transactionRepository.countByStatus("SUCCESSFUL");
        long failed = transactionRepository.countByStatus("FAILED");

        dto.setSuccessCount(success);
        dto.setFailedCount(failed);

        dto.setTotalTransactions(total);
        dto.setFraudCount(fraud);
        dto.setAlertCount(alert);

        // fraud rate
        double rate = total == 0 ? 0 : ((double) fraud / total) * 100;
        dto.setFraudRate(rate);

        // distribution
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("VALID", valid);
        distribution.put("ALERT", alert);
        distribution.put("FRAUD", fraud);

        dto.setStatusDistribution(distribution);

        // recent frauds
        List<TransactionLog> recentLogs =
                transactionLogRepository.findTop5ByStatusOrderByTimestampDesc("FRAUD");

        List<Map<String, Object>> recentFrauds = recentLogs.stream()
                .map(log -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("accountTo", log.getAccountTo());
                    map.put("amount", log.getAmount());
                    map.put("status", log.getStatus());
                    map.put("reason", log.getReason());
                    return map;
                })
                .collect(Collectors.toList());

        dto.setRecentFrauds(recentFrauds);

        // top patterns (group by reason)
        Map<String, Long> patternMap = transactionLogRepository.findAll().stream()
                .filter(log -> log.getReason() != null && !log.getReason().isEmpty())
                .filter(log -> "FRAUD".equals(log.getStatus()) || "ALERT".equals(log.getStatus()))
                .collect(Collectors.groupingBy(
                        TransactionLog::getReason,
                        Collectors.counting()
                ));

        List<Map<String, Object>> topPatterns = patternMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("reason", e.getKey());
                    map.put("count", e.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        dto.setTopPatterns(topPatterns);

        return dto;
    }
}
