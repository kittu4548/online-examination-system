package com.example.exam.controller;

import com.example.exam.dto.PaymentVerificationRequest;
import com.example.exam.model.CourseEnrollment;
import com.example.exam.model.Subject;
import com.example.exam.model.User;
import com.example.exam.repository.CourseEnrollmentRepository;
import com.example.exam.repository.SubjectRepository;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.EmailService;
import com.example.exam.service.RazorpayService;
import com.razorpay.Order;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private RazorpayService razorpayService;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentController paymentController;

    private User student;
    private Subject paidSubject;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(7L);
        student.setUsername("student");
        student.setEmail("");

        paidSubject = new Subject();
        paidSubject.setId(11L);
        paidSubject.setName("Java");
        paidSubject.setAccessType("PAID");
        paidSubject.setPrice(new BigDecimal("10.00"));
        paidSubject.setSubscriptionDuration("LIFETIME");

        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("student");
        lenient().when(userRepository.findByUsername("student"))
                .thenReturn(Optional.of(student));
        lenient().when(subjectRepository.findById(11L))
                .thenReturn(Optional.of(paidSubject));
        lenient().when(enrollmentRepository
                .findFirstByStudentIdAndSubjectIdAndStatusOrderByPurchasedAtDesc(
                        7L, 11L, "CREATED"))
                .thenReturn(Optional.empty());
    }

    @Test
    void createsOrderForPaidSubjectUsingServerPrice() throws Exception {
        Order order = order("order_test", 1000L, "INR");
        when(enrollmentRepository.existsByStudentIdAndSubjectIdAndStatus(
                7L, 11L, "PAID")).thenReturn(false);
        when(razorpayService.createOrder(
                eq(new BigDecimal("10.00")), any(String.class)))
                .thenReturn(order);
        when(razorpayService.getKeyId()).thenReturn("rzp_test_public");

        ResponseEntity<?> response =
                paymentController.createOrder(11L, authentication);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("order_test", body.get("razorpayOrderId"));
        assertEquals(1000L, body.get("amount"));
        verify(enrollmentRepository).save(any(CourseEnrollment.class));
    }

    @Test
    void rejectsOrderCreationForFreeSubject() {
        paidSubject.setAccessType("FREE");

        ResponseEntity<?> response =
                paymentController.createOrder(11L, authentication);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(razorpayService);
    }

    @Test
    void verifiesPaymentAndCreatesEnrollment() throws Exception {
        CourseEnrollment enrollment = pendingEnrollment();
        when(enrollmentRepository.findByRazorpayPaymentId("pay_test"))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByRazorpayOrderId("order_test"))
                .thenReturn(Optional.of(enrollment));
        when(razorpayService.verifyPayment(
                "order_test", "pay_test", "signature"))
                .thenReturn(true);
        when(razorpayService.fetchOrder("order_test"))
                .thenReturn(order("order_test", 1000L, "INR"));

        ResponseEntity<?> response =
                paymentController.verifyPayment(
                        request("order_test", "pay_test", "signature"),
                        authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) ((Map<?, ?>) response.getBody()).get("success"));
        assertEquals("PAID", enrollment.getStatus());
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void rejectsInvalidSignatureWithoutSavingEnrollment() throws Exception {
        CourseEnrollment enrollment = pendingEnrollment();
        when(enrollmentRepository.findByRazorpayPaymentId("pay_test"))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByRazorpayOrderId("order_test"))
                .thenReturn(Optional.of(enrollment));
        when(razorpayService.verifyPayment(
                "order_test", "pay_test", "bad"))
                .thenReturn(false);

        ResponseEntity<?> response =
                paymentController.verifyPayment(
                        request("order_test", "pay_test", "bad"),
                        authentication);

        assertEquals(400, response.getStatusCode().value());
        assertFalse((Boolean) ((Map<?, ?>) response.getBody()).get("success"));
        verify(enrollmentRepository, never()).save(enrollment);
    }

    @Test
    void rejectsIncorrectRazorpayAmount() throws Exception {
        CourseEnrollment enrollment = pendingEnrollment();
        when(enrollmentRepository.findByRazorpayPaymentId("pay_test"))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByRazorpayOrderId("order_test"))
                .thenReturn(Optional.of(enrollment));
        when(razorpayService.verifyPayment(
                "order_test", "pay_test", "signature"))
                .thenReturn(true);
        when(razorpayService.fetchOrder("order_test"))
                .thenReturn(order("order_test", 999L, "INR"));

        ResponseEntity<?> response =
                paymentController.verifyPayment(
                        request("order_test", "pay_test", "signature"),
                        authentication);

        assertEquals(400, response.getStatusCode().value());
        assertFalse((Boolean) ((Map<?, ?>) response.getBody()).get("success"));
        verify(enrollmentRepository, never()).save(enrollment);
    }

    @Test
    void handlesDuplicatePaymentVerificationIdempotently() throws Exception {
        CourseEnrollment enrollment = pendingEnrollment();
        enrollment.setRazorpayPaymentId("pay_test");
        enrollment.setStatus("PAID");
        when(enrollmentRepository.findByRazorpayPaymentId("pay_test"))
                .thenReturn(Optional.of(enrollment));

        ResponseEntity<?> response =
                paymentController.verifyPayment(
                        request("order_test", "pay_test", "signature"),
                        authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) ((Map<?, ?>) response.getBody()).get("success"));
        verify(razorpayService, never()).verifyPayment(any(), any(), any());
    }

    private CourseEnrollment pendingEnrollment() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudent(student);
        enrollment.setSubject(paidSubject);
        enrollment.setRazorpayOrderId("order_test");
        enrollment.setAmount(new BigDecimal("10.00"));
        enrollment.setStatus("CREATED");
        return enrollment;
    }

    private PaymentVerificationRequest request(
            String orderId, String paymentId, String signature) throws Exception {
        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setSubjectId(11L);
        request.setRazorpayOrderId(orderId);
        request.setRazorpayPaymentId(paymentId);
        request.setRazorpaySignature(signature);
        return request;
    }

    private Order order(String id, long amount, String currency) throws Exception {
        return new Order(new JSONObject()
                .put("id", id)
                .put("amount", amount)
                .put("currency", currency));
    }
}
