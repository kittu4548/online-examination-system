package com.example.exam.service;

import com.example.exam.config.RazorpayConfig;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import com.razorpay.Utils;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RazorpayService {

    private final RazorpayConfig.RazorpayCredentials credentials;

    public RazorpayService(
            RazorpayConfig.RazorpayCredentials credentials) {
        this.credentials = credentials;
    }

    public Order createOrder(
            BigDecimal amount,
            String receipt
    ) throws Exception {

        requireConfigured();

        RazorpayClient razorpayClient =
                new RazorpayClient(
                        credentials.getKeyId(),
                        credentials.getKeySecret()
                );

        // Convert rupees to paise
        long amountInPaise = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receipt);

        return razorpayClient.orders.create(orderRequest);
    }

    public Order fetchOrder(String orderId) throws Exception {
        requireConfigured();
        return new RazorpayClient(
                credentials.getKeyId(),
                credentials.getKeySecret()
        ).orders.fetch(orderId);
    }

    public boolean verifyPayment(
        String orderId,
        String paymentId,
        String signature
) throws Exception {

    requireConfigured();

    JSONObject attributes = new JSONObject();

    attributes.put("razorpay_order_id", orderId);
    attributes.put("razorpay_payment_id", paymentId);
    attributes.put("razorpay_signature", signature);

    return Utils.verifyPaymentSignature(
            attributes,
            credentials.getKeySecret()
    );
}

private void requireConfigured() {
    if (!credentials.isConfigured()) {
        throw new IllegalStateException(
                "Razorpay is not configured. Set RAZORPAY_KEY_ID and "
                        + "RAZORPAY_KEY_SECRET before accepting payments.");
    }
}

public String getKeyId() {
    requireConfigured();
    return credentials.getKeyId();
}
}