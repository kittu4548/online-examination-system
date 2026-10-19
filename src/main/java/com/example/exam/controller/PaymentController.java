package com.example.exam.controller;

import com.example.exam.dto.PaymentVerificationRequest;
import com.example.exam.model.CourseEnrollment;
import com.example.exam.model.Subject;
import com.example.exam.model.User;
import com.example.exam.repository.CourseEnrollmentRepository;
import com.example.exam.repository.SubjectRepository;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.RazorpayService;
import com.razorpay.Order;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    public PaymentController(
            SubjectRepository subjectRepository,
            RazorpayService razorpayService,
            CourseEnrollmentRepository enrollmentRepository,
            UserRepository userRepository) {

        this.subjectRepository = subjectRepository;
        this.razorpayService = razorpayService;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
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

            String receipt =
                    "SUBJECT-" + subjectId + "-"
                            + System.currentTimeMillis();

            Order order = razorpayService.createOrder(
                    subject.getPrice(),
                    receipt
            );

            JSONObject orderJson = order.toJson();

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "keyId",
                    razorpayService.getKeyId()
            );

            response.put(
                    "razorpayOrderId",
                    orderJson.getString("id")
            );

            response.put(
                    "orderId",
                    orderJson.getString("id")
            );

            response.put(
                    "amount",
                    orderJson.getLong("amount")
            );

            response.put(
                    "currency",
                    orderJson.getString("currency")
            );

            response.put(
                    "subjectId",
                    subject.getId()
            );

            response.put(
                    "subjectName",
                    subject.getName()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

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

            // -------------------------------------------------
            // 6. Prevent duplicate payment
            // -------------------------------------------------

            if (enrollmentRepository
                    .findByRazorpayPaymentId(
                            request.getRazorpayPaymentId()
                    )
                    .isPresent()) {

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "message",
                                "Payment already verified"
                        )
                );
            }

            // -------------------------------------------------
            // 7. Create enrollment
            // -------------------------------------------------

            CourseEnrollment enrollment =
                    new CourseEnrollment();

            enrollment.setStudent(student);

            enrollment.setSubject(subject);

            enrollment.setRazorpayOrderId(
                    request.getRazorpayOrderId()
            );

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
}