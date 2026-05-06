package com.fraud_detection.Fraud_Management.graph;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class GraphInitializer
        implements CommandLineRunner {

    private final Neo4jGraphLoaderService loaderService;

    public GraphInitializer(Neo4jGraphLoaderService loaderService) {
        this.loaderService = loaderService;
    }

    @Override
    public void run(String... args) {

        loaderService.loadHistoricalData();
    }
}