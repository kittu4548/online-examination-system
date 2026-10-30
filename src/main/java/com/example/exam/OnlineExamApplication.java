
package com.example.exam;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class OnlineExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineExamApplication.class, args);
    }

    @Bean
    CommandLineRunner createAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {

        return args -> {

            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                System.out.println(">>> Admin bootstrap skipped: credentials are not configured. <<<");
                return;
            }

            String adminUsername = username.trim();
            String adminEmail = email.trim();

            // Check whether the username or email is already registered.
            if (userRepository.findByUsername(adminUsername).isPresent()
                    || userRepository.findByEmail(adminEmail).isPresent()) {

                System.out.println(
                    ">>> Admin username or email already exists. <<<"
                );
                return;
            }

            User adminUser = new User();

            adminUser.setUsername(adminUsername);
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(
                passwordEncoder.encode(password)
            );
            adminUser.setRole("ADMIN");
            adminUser.setFullName("Admin");

            userRepository.save(adminUser);

            System.out.println(">>> Configured bootstrap admin created. <<<");
        };
    }
}
