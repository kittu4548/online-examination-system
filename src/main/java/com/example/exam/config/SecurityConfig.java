
package com.example.exam.config;

import com.example.exam.service.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CustomUserDetailsService userDetailsService)
            throws Exception {

        http
            // Keep CSRF protection enabled (Spring Security default)
            .authorizeHttpRequests(authorize -> authorize

                // Public static resources
                .requestMatchers(
                    "/js/**",
                    "/static/js/**",
                    "/css/**",
                    "/images/**",
                    "/uploads/**"
                ).permitAll()

                // Public pages
                .requestMatchers(
                    "/",
                    "/login",
                    "/register",
                    "/h2-console/**",
                    "/error"
                ).permitAll()

                // Admin pages: admin only
                .requestMatchers("/admin/payments")
    .hasAuthority("ROLE_ADMIN")

.requestMatchers("/admin/**")
    .hasAuthority("ROLE_ADMIN")

                // Dashboard accessible to both roles
                .requestMatchers("/student/dashboard")
                    .hasAnyRole("ADMIN", "STUDENT")

                // Student and exam pages
                .requestMatchers("/student/**", "/exam/**")
                    .hasRole("STUDENT")

                // Other authenticated pages
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {

                    boolean isAdmin = authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_ADMIN")
                        );

                    if (isAdmin) {
                        response.sendRedirect(
                            request.getContextPath() + "/admin/dashboard"
                        );
                    } else {
                        response.sendRedirect(
                            request.getContextPath() + "/student/dashboard"
                        );
                    }
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutRequestMatcher(
                    new AntPathRequestMatcher("/logout")
                )
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )

            .userDetailsService(userDetailsService)

            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())
            );

        return http.build();
    }
}
