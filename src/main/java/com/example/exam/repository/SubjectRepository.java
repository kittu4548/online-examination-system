
package com.example.exam.repository;

import com.example.exam.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository
        extends JpaRepository<Subject, Long> {

    List<Subject> findByPublishedTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
