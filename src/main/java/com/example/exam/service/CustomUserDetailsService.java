package com.example.exam.service;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        System.out.println(
                "========== SPRING SECURITY USER LOAD =========="
        );

        System.out.println(
                "Login username: " + username
        );

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username
                        )
                );

        System.out.println(
                "Database user: " + user.getUsername()
        );

        System.out.println(
                "Database email: " + user.getEmail()
        );

        System.out.println(
                "Database role BEFORE conversion: [" +
                        user.getRole() + "]"
        );

        String role = user.getRole();

        if (role == null || role.trim().isEmpty()) {

            throw new UsernameNotFoundException(
                    "User has no role in database: " + username
            );
        }

        role = role.trim().toUpperCase(Locale.ROOT);

        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        System.out.println(
                "Spring Security authority: [" + role + "]"
        );

        System.out.println(
                "================================================"
        );

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(
                        new SimpleGrantedAuthority(role)
                )
        );
    }
}