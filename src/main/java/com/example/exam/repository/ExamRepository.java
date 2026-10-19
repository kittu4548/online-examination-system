
package com.example.exam.repository;

import com.example.exam.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    // Find all exams assigned to a particular subject.
    List<Exam> findBySubjectId(Long subjectId);

}
