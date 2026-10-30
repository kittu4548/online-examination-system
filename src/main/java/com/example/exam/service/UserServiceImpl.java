
package com.example.exam.service;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void saveStudent(User user) {

        if (user.getUsername() == null
                || user.getUsername().isBlank()) {
            throw new IllegalArgumentException(
                "Username is required."
            );
        }

        if (user.getEmail() == null
                || user.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                "Email is required."
            );
        }

        if (user.getPassword() == null
                || user.getPassword().isBlank()) {
            throw new IllegalArgumentException(
                "Password is required."
            );
        }

        String username =
            user.getUsername().trim().toLowerCase();

        String email =
            user.getEmail().trim().toLowerCase();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException(
                "Username already exists."
            );
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                "Email already exists."
            );
        }

        user.setUsername(username);
        user.setEmail(email);

        // Store a BCrypt hash, never the plain-text password.
        user.setPassword(
            passwordEncoder.encode(user.getPassword())
        );

        // Store the role in the format accepted by the database.
        user.setRole("STUDENT");

        userRepository.save(user);
    }

    @Override
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        return userRepository
            .findByUsername(username.trim().toLowerCase())
            .orElse(null);
    }
}
