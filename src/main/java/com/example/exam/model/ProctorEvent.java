package com.example.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "proctor_events")
public class ProctorEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;
    @Column(nullable = false, length = 40)
    private String eventType;
    @Column(length = 500)
    private String details;
    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();
    public Long getId(){return id;} public User getStudent(){return student;} public void setStudent(User v){student=v;}
    public Exam getExam(){return exam;} public void setExam(Exam v){exam=v;}
    public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    public String getDetails(){return details;} public void setDetails(String v){details=v;}
    public LocalDateTime getOccurredAt(){return occurredAt;} public void setOccurredAt(LocalDateTime v){occurredAt=v;}
}
