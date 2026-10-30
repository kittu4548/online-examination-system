package com.example.exam.service;

import com.example.exam.model.CourseEnrollment;
import com.example.exam.model.Exam;
import com.example.exam.model.User;
import com.example.exam.repository.CourseEnrollmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CourseAccessService {

    private final CourseEnrollmentRepository enrollmentRepository;

    public CourseAccessService(
            CourseEnrollmentRepository enrollmentRepository) {

        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Checks whether the student can access an exam.
     */
    public boolean canAccessExam(
            User student,
            Exam exam) {

        // Exam without a subject:
        // keep existing access behaviour.
        if (exam.getSubject() == null) {
            return true;
        }

        // FREE subject
        if ("FREE".equalsIgnoreCase(
                exam.getSubject().getAccessType())) {

            return true;
        }

        // PAID subject
        Optional<CourseEnrollment> enrollment =
                enrollmentRepository
                        .findByStudentIdAndSubjectIdAndStatus(
                                student.getId(),
                                exam.getSubject().getId(),
                                "PAID"
                        );

        if (enrollment.isEmpty()) {
            return false;
        }

        CourseEnrollment purchase =
                enrollment.get();

        // Lifetime access
        if (purchase.getExpiryDate() == null) {
            return true;
        }

        // Check expiry
        return purchase.getExpiryDate()
                .isAfter(LocalDateTime.now());
    }
}