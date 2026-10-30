package com.example.exam.controller;

import com.example.exam.dto.PaymentVerificationRequest;
import com.example.exam.model.CourseEnrollment;
import com.example.exam.model.Subject;
import com.example.exam.model.User;
import com.example.exam.repository.CourseEnrollmentRepository;
import com.example.exam.repository.SubjectRepository;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.RazorpayService;
import com.example.exam.service.EmailService;
import com.razorpay.Order;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/student/payment")
public class PaymentController {

    private final SubjectRepository subjectRepository;
    private final RazorpayService razorpayService;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public PaymentController(
            SubjectRepository subjectRepository,
            RazorpayService razorpayService,
            CourseEnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            EmailService emailService) {

        this.subjectRepository = subjectRepository;
        this.razorpayService = razorpayService;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // =========================================================
    // CREATE RAZORPAY ORDER
    // POST /student/payment/create/{subjectId}
    // =========================================================

    @PostMapping("/create/{subjectId}")
    public ResponseEntity<?> createOrder(
            @PathVariable Long subjectId,
            Authentication authentication) {

        try {

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error",
                                "User is not authenticated"
                        ));
            }

            Subject subject = subjectRepository
                    .findById(subjectId)
                    .orElseThrow(() ->
                            new RuntimeException("Subject not found"));

            // Only PAID subjects should use Razorpay
            if (!"PAID".equalsIgnoreCase(subject.getAccessType())) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "This subject is free"
                        ));
            }

            // Validate price
            if (subject.getPrice() == null ||
                    subject.getPrice()
                            .compareTo(BigDecimal.ZERO) <= 0) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Invalid subject price"
                        ));
            }

            User student = userRepository
                    .findByUsername(authentication.getName())
                    .orElseThrow(() ->
                            new RuntimeException("Student not found"));

            // Prevent paying again if already enrolled
            boolean alreadyEnrolled =
                    enrollmentRepository
                            .existsByStudentIdAndSubjectIdAndStatus(
                                    student.getId(),
                                    subject.getId(),
                                    "PAID"
                            );

            if (alreadyEnrolled) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "You have already purchased this subject"
                        ));
            }

            CourseEnrollment pendingEnrollment =
                            enrollmentRepository
                                    .findFirstByStudentIdAndSubjectIdAndStatusOrderByPurchasedAtDesc(
                                            student.getId(), subject.getId(), "CREATED")
                                    .orElse(null);

            if (pendingEnrollment != null
                            && pendingEnrollment.getRazorpayOrderId() != null) {
                        return ResponseEntity.ok(orderResponse(
                                pendingEnrollment.getRazorpayOrderId(),
                                subject,
                                pendingEnrollment.getAmount(),
                                pendingEnrollment.getAmount()
                                        .movePointRight(2)
                                        .longValueExact(),
                                "INR"
                        ));
            }

            String receipt =
                    "SUBJECT-" + subjectId + "-"
                            + System.currentTimeMillis();

            Order order = razorpayService.createOrder(
                    subject.getPrice(),
                    receipt
            );

            JSONObject orderJson = order.toJson();
            pendingEnrollment = new CourseEnrollment();
            pendingEnrollment.setStudent(student);
            pendingEnrollment.setSubject(subject);
            pendingEnrollment.setRazorpayOrderId(orderJson.getString("id"));
            pendingEnrollment.setAmount(subject.getPrice());
            pendingEnrollment.setStatus("CREATED");
            pendingEnrollment.setPurchasedAt(LocalDateTime.now());
            enrollmentRepository.save(pendingEnrollment);

            return ResponseEntity.ok(orderResponse(
                    orderJson.getString("id"),
                    subject,
                    pendingEnrollment.getAmount(),
                    orderJson.getLong("amount"),
                    orderJson.getString("currency")
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            "Unable to create payment order"
                    ));
        }
    }

    // =========================================================
    // VERIFY RAZORPAY PAYMENT
    // POST /student/payment/verify
    // =========================================================

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<?> verifyPayment(
            @RequestBody PaymentVerificationRequest request,
            Authentication authentication) {

        try {

            if (authentication == null ||
                    !authentication.isAuthenticated()) {

                return ResponseEntity.status(401)
                        .body(Map.of(
                                "success", false,
                                "message",
                                "User is not authenticated"
                        ));
            }

            // -------------------------------------------------
            // 1. Validate request
            // -------------------------------------------------

            if (request.getRazorpayOrderId() == null ||
                    request.getRazorpayPaymentId() == null ||
                    request.getRazorpaySignature() == null ||
                    request.getSubjectId() == null) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message",
                                "Invalid payment information"
                        ));
            }

            // -------------------------------------------------
            // 2. Find subject
            // -------------------------------------------------

            Subject subject = subjectRepository
                    .findById(request.getSubjectId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Subject not found"
                            ));

            // -------------------------------------------------
            // 3. Make sure subject is paid
            // -------------------------------------------------

            if (!"PAID".equalsIgnoreCase(
                    subject.getAccessType())) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message",
                                "This subject does not require payment"
                        ));
            }

            // -------------------------------------------------
            // 4. Get logged-in student
            // -------------------------------------------------

            User student = userRepository
                    .findByUsername(authentication.getName())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Student not found"
                            ));

            CourseEnrollment existingPayment =
                    enrollmentRepository.findByRazorpayPaymentId(
                            request.getRazorpayPaymentId()).orElse(null);
            if (existingPayment != null) {
                if (student.equals(existingPayment.getStudent())
                        && subject.equals(existingPayment.getSubject())) {
                    return ResponseEntity.ok(
                            Map.of(
                                    "success", true,
                                    "message", "Payment already verified"
                            ));
                }
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Payment is already associated with another enrollment"
                        ));
            }

            CourseEnrollment enrollment =
                    enrollmentRepository
                            .findByRazorpayOrderId(
                                    request.getRazorpayOrderId())
                            .orElse(null);

            if (enrollment == null
                    || !student.equals(enrollment.getStudent())
                    || !subject.equals(enrollment.getSubject())
                    || enrollment.getAmount() == null
                    || enrollment.getAmount().compareTo(subject.getPrice()) != 0
                    || !"CREATED".equalsIgnoreCase(enrollment.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message",
                                "Payment order does not match this subject or student"
                        ));
            }

            // -------------------------------------------------
            // 5. Verify Razorpay signature
            // -------------------------------------------------

            boolean verified =
                    razorpayService.verifyPayment(
                            request.getRazorpayOrderId(),
                            request.getRazorpayPaymentId(),
                            request.getRazorpaySignature()
                    );

            if (!verified) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message",
                                "Payment verification failed"
                        ));
            }

            Order razorpayOrder =
                    razorpayService.fetchOrder(
                            request.getRazorpayOrderId());
            JSONObject verifiedOrder = razorpayOrder.toJson();
            long expectedAmountInPaise = enrollment.getAmount()
                    .movePointRight(2)
                    .longValueExact();
            if (verifiedOrder.optLong("amount", -1)
                    != expectedAmountInPaise
                    || !"INR".equalsIgnoreCase(
                            verifiedOrder.optString("currency"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message",
                                "Payment amount or currency does not match the order"
                        ));
            }

            // -------------------------------------------------
            // 6. Complete the persisted enrollment
            // -------------------------------------------------
            enrollment.setRazorpayPaymentId(
                    request.getRazorpayPaymentId()
            );

            enrollment.setAmount(
                    subject.getPrice()
            );

            enrollment.setStatus("PAID");

            LocalDateTime purchasedAt =
                    LocalDateTime.now();

            enrollment.setPurchasedAt(
                    purchasedAt
            );

            // -------------------------------------------------
            // 8. Calculate expiry
            // -------------------------------------------------

            String duration =
                    subject.getSubscriptionDuration();

            if ("MONTHLY".equalsIgnoreCase(duration)) {

                enrollment.setExpiryDate(
                        purchasedAt.plusMonths(1)
                );

            } else if ("YEARLY".equalsIgnoreCase(duration)) {

                enrollment.setExpiryDate(
                        purchasedAt.plusYears(1)
                );

            } else {

                // LIFETIME or NONE
                enrollment.setExpiryDate(null);
            }

            // -------------------------------------------------
            // 9. Save enrollment
            // -------------------------------------------------

            enrollmentRepository.save(enrollment);

            // Send a receipt to the account that completed payment.
            // Email failure must not undo a verified payment/enrollment.
            try {
                if (!Boolean.TRUE.equals(
                        enrollment.getConfirmationEmailSent())
                        && student.getEmail() != null
                        && !student.getEmail().isBlank()) {
                    emailService.sendPaymentConfirmation(
                            student.getEmail(), student.getFullName(),
                            subject.getName(), subject.getPrice(),
                            request.getRazorpayPaymentId(), purchasedAt);
                    enrollment.setConfirmationEmailSent(true);
                    enrollmentRepository.save(enrollment);
                }
            } catch (Exception mailError) {
                System.err.println("Payment verified, but confirmation email failed: "
                        + mailError.getMessage());
            }

            // -------------------------------------------------
            // 10. Return success
            // -------------------------------------------------

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Payment verified successfully"
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message",
                            "Payment verification failed"
                    ));
        }
    }

    private Map<String, Object> orderResponse(
            String orderId,
            Subject subject,
            BigDecimal amount,
            long amountInPaise,
            String currency) {
        Map<String, Object> response = new HashMap<>();
        response.put("keyId", razorpayService.getKeyId());
        response.put("razorpayOrderId", orderId);
        response.put("orderId", orderId);
        response.put("amount", amountInPaise);
        response.put("currency", currency);
        response.put("subjectId", subject.getId());
        response.put("subjectName", subject.getName());
        return response;
    }
}