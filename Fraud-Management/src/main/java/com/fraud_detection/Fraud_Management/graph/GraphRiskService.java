package com.fraud_detection.Fraud_Management.graph;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GraphRiskService {

    private final Driver driver;

    public GraphRiskService(Driver driver) {
        this.driver = driver;
    }

    public double calculateGraphRisk(String accountNumber) {

        try (Session session = driver.session()) {

            Map<String, Object> params =
                    new HashMap<>();

            params.put("account", accountNumber);

            Result result = session.run("""

                MATCH (a:Account)-[r:TRANSFERRED]->()

                WHERE a.accountNumber = $account
                AND r.status = 'FRAUD'

                RETURN COUNT(r) AS fraudCount

                """, params);

            Record record = result.single();

            int fraudCount =
                    record.get("fraudCount").asInt();

            System.out.println(
                    "Neo4j Fraud Count: " + fraudCount
            );

            //  Graph risk scoring
            if (fraudCount >= 10) {
                return 0.9;
            }

            if (fraudCount >= 5) {
                return 0.6;
            }

            if (fraudCount >= 2) {
                return 0.3;
            }

            return 0.0;
        }
    }
}