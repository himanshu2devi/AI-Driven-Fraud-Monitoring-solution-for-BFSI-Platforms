package com.fraud_detection.Fraud_Management.graph;

import com.fraud_detection.Fraud_Management.entity.TransactionLog;
import com.fraud_detection.Fraud_Management.repository.TransactionLogRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Neo4jGraphLoaderService {

    private final TransactionLogRepository logRepository;

    private final Driver driver;

    public Neo4jGraphLoaderService(
            TransactionLogRepository logRepository,
            Driver driver
    ) {
        this.logRepository = logRepository;
        this.driver = driver;
    }

    public void loadHistoricalData() {

        List<TransactionLog> logs =
                logRepository.findAll();

        System.out.println(
                "\nLoading historical graph into Neo4j..."
        );

        try (Session session = driver.session()) {

            for (TransactionLog log : logs) {

                // Skip invalid records
                if (log.getAccountFrom() == null
                        || log.getAccountTo() == null) {
                    continue;
                }

                // HashMap allows null values
                Map<String, Object> params = new HashMap<>();

                params.put("fromAcc", log.getAccountFrom());
                params.put("toAcc", log.getAccountTo());
                params.put("transactionId", log.getTransactionId());
                params.put("amount", log.getAmount());
                params.put("transactionType", log.getTransactionType());
                params.put("status", log.getStatus());
                params.put("reason", log.getReason());

                params.put(
                        "timestamp",
                        log.getTimestamp() != null
                                ? log.getTimestamp().toString()
                                : null
                );

                session.run("""

                    MERGE (from:Account {
                        accountNumber:$fromAcc
                    })

                    MERGE (to:Account {
                        accountNumber:$toAcc
                    })

                    CREATE (from)-[:TRANSFERRED {

                        transactionId:$transactionId,
                        amount:$amount,
                        transactionType:$transactionType,
                        status:$status,
                        reason:$reason,
                        timestamp:$timestamp

                    }]->(to)

                    """,
                        params
                );
            }
        }

        System.out.println(
                "Neo4j graph loaded successfully\n"
        );
    }

    public void saveTransactionToGraph(
            TransactionLog log
    ) {

        try (Session session = driver.session()) {

            Map<String, Object> params =
                    new HashMap<>();

            params.put("fromAcc", log.getAccountFrom());
            params.put("toAcc", log.getAccountTo());
            params.put("transactionId", log.getTransactionId());
            params.put("amount", log.getAmount());
            params.put("transactionType", log.getTransactionType());
            params.put("status", log.getStatus());
            params.put("reason", log.getReason());

            params.put(
                    "timestamp",
                    log.getTimestamp() != null
                            ? log.getTimestamp().toString()
                            : null
            );

            session.run("""

            MERGE (from:Account {
                accountNumber:$fromAcc
            })

            MERGE (to:Account {
                accountNumber:$toAcc
            })

            CREATE (from)-[:TRANSFERRED {

                transactionId:$transactionId,
                amount:$amount,
                transactionType:$transactionType,
                status:$status,
                reason:$reason,
                timestamp:$timestamp

            }]->(to)

            """,
                    params
            );

            System.out.println(
                    "Live transaction added to Neo4j"
            );
        }
    }
}