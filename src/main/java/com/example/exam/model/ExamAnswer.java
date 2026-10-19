package com.example.exam.model;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_answers")
public class ExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /***
     Subscribe Lazycoder - https://www.youtube.com/c/LazyCoderOnline?sub_confirmation=1
     whatsapp - https://wa.me/919572181024
     email - wapka1503@gmail.com
     ***/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_result_id", nullable = false)
    private ExamResult examResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // The option selected by the student (0 if unanswered)
    @Column(nullable = false)
    private int selectedOption;

    // Constructors
    public ExamAnswer() {
    }

    public ExamAnswer(ExamResult examResult, Question question, int selectedOption) {
        this.examResult = examResult;
        this.question = question;
        this.selectedOption = selectedOption;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ExamResult getExamResult() { return examResult; }
    public void setExamResult(ExamResult examResult) { this.examResult = examResult; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public int getSelectedOption() { return selectedOption; }
    public void setSelectedOption(int selectedOption) { this.selectedOption = selectedOption; }
}
