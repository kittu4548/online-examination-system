
package com.example.exam;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class OnlineExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineExamApplication.class, args);
    }

    @Bean
    CommandLineRunner createAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            String adminUsername = "admin";
            String adminEmail = "admin@example.com";

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
                passwordEncoder.encode("adminpass")
            );
            adminUser.setRole("ADMIN");
            adminUser.setFullName("Admin");

            userRepository.save(adminUser);

            System.out.println(
                ">>> Admin user created: admin / adminpass <<<"
            );
        };
    }
}
