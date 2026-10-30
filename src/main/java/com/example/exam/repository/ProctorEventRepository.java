package com.example.exam.repository;
import com.example.exam.model.ProctorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProctorEventRepository extends JpaRepository<ProctorEvent,Long> {
 List<ProctorEvent> findTop300ByOrderByOccurredAtDesc();
 long countByEventType(String eventType);
}
