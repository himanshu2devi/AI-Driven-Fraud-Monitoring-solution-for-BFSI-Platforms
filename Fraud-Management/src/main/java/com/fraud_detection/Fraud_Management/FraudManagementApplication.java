package com.fraud_detection.Fraud_Management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@ComponentScan(basePackages = "com.fraud_detection.Fraud_Management")
public class FraudManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(FraudManagementApplication.class, args);
	}

}
