package com.example.exam.service;

import com.example.exam.model.CourseEnrollment;
import com.example.exam.model.Exam;
import com.example.exam.model.Subject;
import com.example.exam.model.User;
import com.example.exam.repository.CourseEnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessServiceTest {

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseAccessService courseAccessService;

    @Test
    void allowsFreeSubjectExamWithoutEnrollment() {
        User student = new User();
        Subject subject = new Subject();
        subject.setAccessType("FREE");
        Exam exam = new Exam();
        exam.setSubject(subject);

        assertTrue(courseAccessService.canAccessExam(student, exam));
    }

    @Test
    void allowsPaidSubjectWithLifetimeEnrollment() {
        User student = new User();
        student.setId(7L);
        Subject subject = new Subject();
        subject.setId(11L);
        subject.setAccessType("PAID");
        Exam exam = new Exam();
        exam.setSubject(subject);

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setExpiryDate(null);
        when(enrollmentRepository.findByStudentIdAndSubjectIdAndStatus(
                7L, 11L, "PAID")).thenReturn(Optional.of(enrollment));

        assertTrue(courseAccessService.canAccessExam(student, exam));
    }

    @Test
    void deniesExpiredPaidSubjectEnrollment() {
        User student = new User();
        student.setId(7L);
        Subject subject = new Subject();
        subject.setId(11L);
        subject.setAccessType("PAID");
        Exam exam = new Exam();
        exam.setSubject(subject);

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(enrollmentRepository.findByStudentIdAndSubjectIdAndStatus(
                7L, 11L, "PAID")).thenReturn(Optional.of(enrollment));

        assertFalse(courseAccessService.canAccessExam(student, exam));
    }
}
