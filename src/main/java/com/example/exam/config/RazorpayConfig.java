package com.example.exam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayCredentials razorpayCredentials() {
        return new RazorpayCredentials(keyId, keySecret);
    }

    public static class RazorpayCredentials {

        private final String keyId;
        private final String keySecret;

        public RazorpayCredentials(String keyId, String keySecret) {
            this.keyId = keyId;
            this.keySecret = keySecret;
        }

        public String getKeyId() {
            return keyId;
        }

        public String getKeySecret() {
            return keySecret;
        }
    }
}