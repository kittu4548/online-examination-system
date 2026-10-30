package com.example.exam.controller;

import com.example.exam.model.*;
import com.example.exam.repository.*;
import com.example.exam.service.CourseAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamAnswerRepository examAnswerRepository;

    // Step 5: Course access/security service
    @Autowired
    private CourseAccessService courseAccessService;


    // ============================================================
    // OPEN EXAM
    // ============================================================

    @GetMapping("/{examId}")
    public String getExamPage(
            @PathVariable Long examId,
            Model model,
            HttpSession session) {

        // 1. Get currently logged-in user
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String currentUsername = authentication.getName();

        User student = userRepository
                .findByUsername(currentUsername)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        model.addAttribute("student", student);


        // 2. Find the exam
        Exam exam = examRepository
                .findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam Id: " + examId));


        // ========================================================
        // STEP 5 SECURITY CHECK
        // ========================================================

        /*
         * FREE subject:
         *      Allow access.
         *
         * PAID subject:
         *      Student must have a valid PAID enrollment.
         *
         * Expired enrollment:
         *      Access denied.
         */
        if (!courseAccessService.canAccessExam(
                student,
                exam)) {

            model.addAttribute(
                    "error",
                    "Please purchase this course to access this exam."
            );

            return "student/access-denied";
        }


        // 3. Add exam to model
        model.addAttribute("exam", exam);


        // 4. Load questions
        List<Question> questions = exam.getQuestions();

        // Force-load lazy collection
        questions.size();

        model.addAttribute(
                "questions",
                questions
        );
        session.setAttribute("examStart:" + examId, LocalDateTime.now());


        // 5. Show exam page
        return "student/exam_page";
    }


    // ============================================================
    // SUBMIT EXAM
    // ============================================================

    @PostMapping("/submit")
    @Transactional
    public String submitExam(
            @RequestParam Long examId,
            @RequestParam Map<String, String> submittedAnswers,
            RedirectAttributes redirectAttributes,
            HttpSession session) {


        // 1. Get logged-in user
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String currentUsername = authentication.getName();

        User student = userRepository
                .findByUsername(currentUsername)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));


        // 2. Find exam
        Exam exam = examRepository
                .findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam Id: " + examId));


        // ========================================================
        // STEP 5 SECURITY CHECK
        // ========================================================

        /*
         * IMPORTANT:
         *
         * We check access again during submission.
         *
         * This prevents someone from bypassing the exam page
         * and directly sending a POST request to:
         *
         *      /exam/submit
         *
         * for a paid exam.
         */
        if (!courseAccessService.canAccessExam(
                student,
                exam)) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "You do not have access to this exam. Please purchase the course first."
            );

            return "redirect:/student/dashboard";
        }

        if (examResultRepository.existsByStudentIdAndExamId(
                student.getId(), exam.getId())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "You have already submitted this exam.");
            return "redirect:/student/dashboard";
        }

        LocalDateTime startedAt =
                (LocalDateTime) session.getAttribute("examStart:" + examId);
        if (startedAt != null
                && Duration.between(startedAt, LocalDateTime.now()).getSeconds()
                > exam.getDurationInMinutes() * 60L + 60L) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "The exam time has expired.");
            return "redirect:/student/dashboard";
        }

        boolean containsForeignQuestion = submittedAnswers.keySet().stream()
                .filter(key -> key.startsWith("q_"))
                .anyMatch(key -> exam.getQuestions().stream()
                        .noneMatch(question ->
                                ("q_" + question.getId()).equals(key)));
        if (containsForeignQuestion) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Invalid question submitted for this exam.");
            return "redirect:/student/dashboard";
        }


        // ========================================================
        // CALCULATE RESULT
        // ========================================================

        int totalMarks = 0;
        int scoreAchieved = 0;


        // 3. Create result
        ExamResult result = new ExamResult();

        result.setStudent(student);
        result.setExam(exam);
        result.setSubmissionTime(
                LocalDateTime.now()
        );


        // 4. Save first to generate result ID
        examResultRepository.save(result);


        // ========================================================
        // PROCESS QUESTIONS
        // ========================================================

        for (Question question : exam.getQuestions()) {

            // Add question marks to total
            totalMarks += question.getMarks();


            // Form field name:
            //
            // q_1
            // q_2
            // q_3
            //
            String answerKey =
                    "q_" + question.getId();


            // 0 means unanswered
            int selectedOption = 0;


            // Check whether student answered
            if (submittedAnswers.containsKey(answerKey)) {

                try {

                    selectedOption =
                            Integer.parseInt(
                                    submittedAnswers
                                            .get(answerKey)
                            );

                    if (selectedOption < 1 || selectedOption > 4) {
                        selectedOption = 0;
                    }
                } catch (NumberFormatException e) {

                    // Invalid answer
                    selectedOption = 0;
                }


                // Check answer
                if (selectedOption ==
                        question.getCorrectAnswer()) {

                    scoreAchieved +=
                            question.getMarks();
                }
            }


            // ====================================================
            // SAVE STUDENT ANSWER
            // ====================================================

            ExamAnswer examAnswer =
                    new ExamAnswer(
                            result,
                            question,
                            selectedOption
                    );

            examAnswerRepository.save(
                    examAnswer
            );

            result.addAnswer(
                    examAnswer
            );
        }

        // ========================================================
        // SAVE FINAL RESULT
        // ========================================================

        result.setScoreAchieved(
                scoreAchieved
        );

        result.setTotalMarks(
                totalMarks
        );

        examResultRepository.save(
                result
        );


        // ========================================================
        // SEND RESULT DATA TO RESULT PAGE
        // ========================================================

        redirectAttributes.addFlashAttribute(
                "score",
                scoreAchieved
        );

        redirectAttributes.addFlashAttribute(
                "total",
                totalMarks
        );

        redirectAttributes.addFlashAttribute(
                "resultId",
                result.getId()
        );
        session.removeAttribute("examStart:" + examId);


        // Redirect to result page
        return "redirect:/student/result";
    }
}