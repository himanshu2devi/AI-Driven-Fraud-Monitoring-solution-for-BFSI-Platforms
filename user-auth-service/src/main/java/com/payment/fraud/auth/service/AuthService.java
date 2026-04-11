package com.payment.fraud.auth.service;

import com.payment.fraud.auth.dto.AuthRequest;
import com.payment.fraud.auth.dto.RegisterRequest;
import com.payment.fraud.auth.entity.Account;
import com.payment.fraud.auth.entity.Roles;
import com.payment.fraud.auth.entity.User;
import com.payment.fraud.auth.repository.AccountRepository;
import com.payment.fraud.auth.repository.RoleRepository;
import com.payment.fraud.auth.repository.UserRepository;
import com.payment.fraud.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public String register(RegisterRequest req) {

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));

        // 🔥 Controlled roles (IMPORTANT)
        String roleName = req.getRole();

        if (roleName == null || roleName.isBlank()) {
            roleName = "ROLE_USER";
        }

        // ✅ Only allow these roles (prevent garbage input)
        if (!roleName.equals("ROLE_USER") &&
                !roleName.equals("ROLE_FRAUDANALYST") &&
                !roleName.equals("ROLE_ADMIN")) {

            throw new RuntimeException("Invalid role");
        }

        Roles role = roleRepo.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.getRoles().add(role);

        userRepo.save(user);

        return "User registered";
    }

//    public String login(AuthRequest req) {
//
//        System.out.println("password" +req.getPassword());
//        Authentication auth = authManager.authenticate(
//                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
//        System.out.println("auth " +auth.isAuthenticated());
//        return tokenProvider.generateToken(auth.getName());
//    }

//    public String login(AuthRequest req) {
//        User user = userRepo.findByUsername(req.getUsername())
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//
//        if (!user.isEnabled()) {
//            throw new LockedException("Your account is locked. Please contact support.");
//        }
//
//        Account account = accountRepository.findByUserId(user.getId())
//                .orElseThrow(() -> new RuntimeException("Account not found for user"));
//
//        if (!account.getAccountEnabled()) {
//            throw new LockedException("Your bank account is locked. Please contact support.");
//        }
//
//        try {
//            Authentication auth = authManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
//            );
//
//            user.setFailedLoginAttempts(0); // reset after success
//            userRepo.save(user);
//
//            return tokenProvider.generateToken(auth.getName());
//
//        } catch (Exception ex) {
//            int attempts = user.getFailedLoginAttempts() + 1;
//            user.setFailedLoginAttempts(attempts);
//
//            if (attempts >= 3) {
//                user.setEnabled(false);
//                account.setAccountEnabled(false);
//                accountRepository.save(account);
//                userRepo.save(user);
//
//                throw new LockedException("Account locked after 3 failed attempts. Please contact support.");
//            }
//
//            userRepo.save(user);
//
//            throw new BadCredentialsException("Invalid credentials. Attempt " + attempts + " of 3");
//        }
//    }

    public String login(AuthRequest req) {
        User user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new LockedException("Your account is locked. Please contact support.");
        }

        // Check if user is admin or not
        boolean isUser = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ROLE_USER"));

        // For user, check account status
        if (isUser) {
            Account account = accountRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found for user"));

            if (!account.getAccountEnabled()) {
                throw new LockedException("Your bank account is locked. Please contact support.");
            }
        }

        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            user.setFailedLoginAttempts(0); // reset after success
            userRepo.save(user);

            return tokenProvider.generateToken(auth.getName());

        } catch (Exception ex) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 3) {
                user.setEnabled(false);

                // Only disable account if it is user
                if (isUser) {
                    accountRepository.findByUserId(user.getId()).ifPresent(account -> {
                        account.setAccountEnabled(false);
                        accountRepository.save(account);
                    });
                }

                userRepo.save(user);

                throw new LockedException("Account locked after 3 failed attempts. Please contact support.");
            }

            userRepo.save(user);

            throw new BadCredentialsException("Invalid credentials. Attempt " + attempts + " of 3");
        }
    }



}

