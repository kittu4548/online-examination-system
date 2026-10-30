package com.example.exam.service;

import com.example.exam.config.RazorpayConfig.RazorpayCredentials;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RazorpayServiceTest {

    @Test
    void rejectsPaymentVerificationWhenCredentialsAreMissing() {
        RazorpayService service = new RazorpayService(
                new RazorpayCredentials("", ""));

        assertThrows(IllegalStateException.class, () ->
                service.verifyPayment("order_test", "pay_test", "signature"));
    }

    @Test
    void rejectsInvalidPaymentSignature() throws Exception {
        RazorpayService service = new RazorpayService(
                new RazorpayCredentials("rzp_test", "secret"));

        assertFalse(service.verifyPayment(
                "order_test", "pay_test", "invalid-signature"));
    }
}
