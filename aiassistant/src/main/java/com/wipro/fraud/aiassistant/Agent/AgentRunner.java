package com.wipro.fraud.aiassistant.Agent;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class AgentRunner implements CommandLineRunner {

    private volatile boolean agentActive = false;

    private final FraudInvestigationAgent investigationAgent;

    public AgentRunner(FraudInvestigationAgent investigationAgent) {
        this.investigationAgent = investigationAgent;
    }

    @Override
    public void run(String... args) {

        new Thread(() -> {

            Scanner scanner = new Scanner(System.in);

            System.out.println("\n💡 Type 'agent' to start Fraud Investigation Mode");

            while (true) {

                String input = scanner.nextLine();

                // 👉 START AGENT MODE
                if ("agent".equalsIgnoreCase(input)) {

                    if (agentActive) {
                        System.out.println("⚠️ Agent already running");
                        continue;
                    }

                    agentActive = true;

                    System.out.println("\n🤖 Agent Mode Activated");
                    System.out.println("Type 'investigate <account>' or 'help' or 'exit'\n");

                    startAgentLoop(scanner);

                    agentActive = false; // 🔥 reset after exit
                }
            }

        }).start();
    }


    private void startAgentLoop(Scanner scanner) {

        while (true) {

            System.out.print("Agent ➤ ");
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("👋 Exiting Agent Mode...\n");
                return; // 🔥 IMPORTANT
            }

            try {

                System.out.println("\n🧠 Thinking...\n");
                // 🔥 HELP COMMAND
                if ("help".equalsIgnoreCase(input)) {
                    System.out.println("""
Available commands:
- investigate <account>
- investigate-network <account>
- help
- exit
""");
                    continue;
                }

                if (input.startsWith("investigate-network")) {

                    String acc = input.replace("investigate-network", "").trim();
                    investigationAgent.investigateNetwork(acc);

                } else if (input.startsWith("investigate")) {

                    String acc = input.replace("investigate", "").trim();
                    investigationAgent.investigate(acc);

                } else {
                    System.out.println("⚠️ Unknown command");
                }

            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
    }
}