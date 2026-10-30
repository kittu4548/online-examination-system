package com.example.exam.controller;

import com.example.exam.model.ExamAnswer;
import com.example.exam.model.ExamResult;
import com.example.exam.model.Question;
import com.example.exam.repository.ExamResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class ReviewController {

    @Autowired
    private ExamResultRepository examResultRepository;

    @GetMapping("/review/{resultId}")
    public String reviewExam(@PathVariable Long resultId, Model model) {


        ExamResult result = examResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid result Id:" + resultId));

        // 2. Security Check: Ensure the logged-in student owns this result
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        if (!result.getStudent().getUsername().equals(currentUsername)) {
            // If not their result, redirect to dashboard.
            return "redirect:/student/dashboard?error=unauthorized";
        }

        // 3. Get all questions from the exam
        // Force-load the questions
        List<Question> questions = result.getExam().getQuestions();
        questions.size();

        // 4. Get all answers and put them in a Map for easy lookup
        // Map<QuestionID, SelectedOption>
        Map<Long, Integer> studentAnswersMap = result.getAnswers().stream()
                .collect(Collectors.toMap(
                        answer -> answer.getQuestion().getId(),
                        ExamAnswer::getSelectedOption
                ));

        // 5. Send all data to the view
        model.addAttribute("exam", result.getExam());
        model.addAttribute("result", result);
        model.addAttribute("questions", questions);
        model.addAttribute("studentAnswers", studentAnswersMap);

        return "student/review_exam";
    }
}
/***
 Subscribe Lazycoder - https://www.youtube.com/c/LazyCoderOnline?sub_confirmation=1
 whatsapp - https://wa.me/919572181024
 email - wapka1503@gmail.com
 ***/
