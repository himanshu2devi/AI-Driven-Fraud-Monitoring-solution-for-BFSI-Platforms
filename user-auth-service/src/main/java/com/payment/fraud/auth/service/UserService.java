package com.payment.fraud.auth.service;

import com.payment.fraud.auth.entity.Permission;
import com.payment.fraud.auth.entity.User;
import com.payment.fraud.auth.repository.PermissionRepository;
import com.payment.fraud.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public Set<String> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getPermissions()
                .stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    public void assignPermission(Long userId, String permissionName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        permissionName = permissionName.trim();
        System.out.println("Looking for permission: " + permissionName);
        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        user.getPermissions().add(permission);
        userRepository.save(user);
    }

    public void removePermission(Long userId, String permissionName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        permissionName = permissionName.trim();
        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        user.getPermissions().remove(permission);
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}
