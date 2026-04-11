package com.wipro.fraud.aiassistant.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import java.util.List;
import java.util.HashSet;


@Component
public class UserClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public Set<String> getUserRoles(String username) {

        String url = "http://localhost:8080/api/users/roles/" + username;

        try {
            List<String> roles = restTemplate.getForObject(url, List.class);

            return roles != null ? new HashSet<>(roles) : new HashSet<>();

        } catch (Exception e) {
            System.out.println("❌ Error fetching roles: " + e.getMessage());
            return new HashSet<>();
        }
    }
}