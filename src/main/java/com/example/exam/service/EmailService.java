
package com.example.exam.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTestEmail(String recipient, String subject, String message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipient);
        email.setSubject(subject);
        email.setText(message);

        mailSender.send(email);
    }

    public void sendPaymentConfirmation(String recipient, String studentName,
                                        String courseName, BigDecimal amount,
                                        String paymentId, LocalDateTime paidAt) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipient);
        email.setSubject("Payment successful - " + courseName);
        email.setText("Hello " + (studentName == null ? "Student" : studentName)
                + ",\n\nYour payment was verified successfully."
                + "\nCourse: " + courseName
                + "\nAmount: INR " + amount
                + "\nPayment reference: " + paymentId
                + "\nDate: " + paidAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                + "\nEnrollment status: PAID"
                + "\n\nThank you for enrolling.");
        mailSender.send(email);
    }
}
