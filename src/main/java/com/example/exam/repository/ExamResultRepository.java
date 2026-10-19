package com.example.exam.repository;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamResult;
import com.example.exam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findByExam(Exam exam);
    @Transactional
    void deleteByStudent_Id(Long studentId);

    List<ExamResult> findByStudent(User student);
    List<ExamResult> findByStudentOrderBySubmissionTimeDesc(User student);
    List<ExamResult> findTop5ByOrderBySubmissionTimeDesc();

    @Transactional
    void deleteByStudent(User student);
}

