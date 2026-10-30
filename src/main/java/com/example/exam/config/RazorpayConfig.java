package com.example.exam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id:${RAZORPAY_KEY_ID:}}")
    private String keyId;

    @Value("${razorpay.key.secret:${RAZORPAY_KEY_SECRET:}}")
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

        public boolean isConfigured() {
            return keyId != null && !keyId.isBlank()
                    && keySecret != null && !keySecret.isBlank();
        }
    }
}