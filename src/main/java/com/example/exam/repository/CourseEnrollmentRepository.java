package com.example.exam.repository;

import com.example.exam.model.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository
        extends JpaRepository<CourseEnrollment, Long> {

    Optional<CourseEnrollment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<CourseEnrollment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<CourseEnrollment> findByStudentIdAndSubjectIdAndStatus(
            Long studentId,
            Long subjectId,
            String status
    );

    List<CourseEnrollment> findAllByOrderByPurchasedAtDesc();

    boolean existsByStudentIdAndSubjectIdAndStatus(
            Long studentId,
            Long subjectId,
            String status
    );
}