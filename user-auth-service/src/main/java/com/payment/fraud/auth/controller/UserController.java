package com.payment.fraud.auth.controller;

import com.payment.fraud.auth.entity.User;
import com.payment.fraud.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/roles/{username}")
    public Set<String> getUserRoles(@PathVariable String username) {

        User user = userService.findByUsername(username);

        return user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
    }
}