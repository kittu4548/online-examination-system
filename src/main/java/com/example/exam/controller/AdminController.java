package com.example.exam.controller;

import com.example.exam.model.CourseEnrollment;
import com.example.exam.model.Exam;
import com.example.exam.model.ExamResult;
import com.example.exam.model.Question;
import com.example.exam.model.Subject;
import com.example.exam.model.User;

import com.example.exam.repository.CourseEnrollmentRepository;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamResultRepository;
import com.example.exam.repository.QuestionRepository;
import com.example.exam.repository.SubjectRepository;
import com.example.exam.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // ============================================================
    // REPOSITORIES / SERVICES
    // ============================================================

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        List<Exam> exams = examRepository.findAll();
        List<Question> questions = questionRepository.findAll();
        List<ExamResult> submissions = examResultRepository.findAll();

        List<User> students = userRepository.findAll()
                .stream()
                .filter(user ->
                        "STUDENT".equalsIgnoreCase(user.getRole()))
                .collect(Collectors.toList());

        List<Subject> subjects = subjectRepository.findAll();

        model.addAttribute("exams", exams);
        model.addAttribute("students", students);
        model.addAttribute("subjects", subjects);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("totalExams", exams.size());
        model.addAttribute("totalQuestions", questions.size());
        model.addAttribute("totalSubmissions", submissions.size());
        model.addAttribute("results", submissions);

        List<String> chartLabels = new ArrayList<>();
        List<Integer> chartData = new ArrayList<>();
        submissions.stream()
                .collect(Collectors.groupingBy(
                        result -> result.getExam().getTitle(),
                        java.util.LinkedHashMap::new,
                        Collectors.counting()))
                .forEach((title, count) -> {
                    chartLabels.add(title);
                    chartData.add(count.intValue());
                });
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        return "admin/dashboard";
    }


    // ============================================================
    // CREATE ADMIN
    // ============================================================

    @GetMapping("/create-admin")
    public String createAdminPage(Model model) {

        model.addAttribute("user", new User());

        return "admin/create-admin";
    }


    @PostMapping("/create-admin")
    public String createAdmin(
            @ModelAttribute User user,
            RedirectAttributes redirectAttributes) {

        try {

            // Check username
            if (user.getUsername() != null &&
                    userRepository
                            .findByUsername(user.getUsername())
                            .isPresent()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Username or email already exists."
                );

                return "redirect:/admin/create-admin";
            }


            // Check email
            if (user.getEmail() != null &&
                    userRepository
                            .findByEmail(user.getEmail())
                            .isPresent()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Username or email already exists."
                );

                return "redirect:/admin/create-admin";
            }


            // Encrypt password
            user.setPassword(
                    passwordEncoder.encode(user.getPassword())
            );


            // Create ADMIN user
            user.setRole("ADMIN");

            userRepository.save(user);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Admin account created successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to create admin account."
            );
        }

        return "redirect:/admin/create-admin";
    }


    // ============================================================
    // EMAIL CENTER
    // ============================================================

    @GetMapping("/email-center")
    public String emailCenter(Model model) {

        return "admin/email-center";
    }


    // ============================================================
    // PAYMENT REPORT
    // ============================================================

    @GetMapping("/payments")
    public String payments(Model model) {

        List<CourseEnrollment> enrollments =
                courseEnrollmentRepository
                        .findAllByOrderByPurchasedAtDesc();


        // Count successful payments
        long successfulPayments =
                enrollments.stream()
                        .filter(enrollment ->
                                "PAID".equalsIgnoreCase(
                                        enrollment.getStatus()
                                )
                        )
                        .count();


        // Calculate total revenue
        BigDecimal totalRevenue =
                enrollments.stream()
                        .filter(enrollment ->
                                "PAID".equalsIgnoreCase(
                                        enrollment.getStatus()
                                )
                        )
                        .map(CourseEnrollment::getAmount)
                        .filter(amount -> amount != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        // Send data to Thymeleaf
        model.addAttribute(
                "enrollments",
                enrollments
        );

        model.addAttribute(
                "successfulPayments",
                successfulPayments
        );

        model.addAttribute(
                "totalRevenue",
                totalRevenue
        );


        return "admin/payments";
    }


    // ============================================================
    // MANAGE EXAMS
    // ============================================================

    @GetMapping("/exams")
    public String manageExams(Model model) {

        List<Exam> exams = examRepository.findAll();

        model.addAttribute("exams", exams);

        return "admin/manage_exams";
    }


    // ============================================================
    // CREATE EXAM
    // ============================================================

    @GetMapping("/exams/new")
    public String newExam(Model model) {

        model.addAttribute(
                "exam",
                new Exam()
        );

        model.addAttribute(
                "subjects",
                subjectRepository.findAll()
        );

        return "admin/add_exam";
    }


    // ============================================================
    // SAVE EXAM
    // ============================================================

    @PostMapping("/exams/save")
    public String saveExam(
            @ModelAttribute Exam exam,
            RedirectAttributes redirectAttributes) {

        try {

            examRepository.save(exam);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Exam saved successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to save exam."
            );
        }

        return "redirect:/admin/exams";
    }


    // ============================================================
    // EDIT EXAM
    // ============================================================

    @GetMapping("/exams/edit/{id}")
    public String editExam(
            @PathVariable Long id,
            Model model) {

        Exam exam = examRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + id
                        )
                );

        model.addAttribute(
                "exam",
                exam
        );

        model.addAttribute(
                "subjects",
                subjectRepository.findAll()
        );

        return "admin/add_exam";
    }


    // ============================================================
    // DELETE EXAM
    // ============================================================

    @GetMapping("/exams/delete/{id}")
    public String deleteExam(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            examRepository.deleteById(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Exam deleted successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to delete exam."
            );
        }

        return "redirect:/admin/exams";
    }


    // ============================================================
    // ASSIGN SUBJECT TO EXAM
    // ============================================================

    @GetMapping("/exams/{examId}/subject")
    public String assignSubjectPage(
            @PathVariable Long examId,
            Model model) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + examId
                        )
                );

        model.addAttribute(
                "exam",
                exam
        );

        model.addAttribute(
                "subjects",
                subjectRepository.findAll()
        );

        return "admin/assign-subject";
    }


    @PostMapping("/exams/{examId}/subject")
    public String assignSubject(
            @PathVariable Long examId,
            @RequestParam(required = false) Long subjectId,
            RedirectAttributes redirectAttributes) {

        try {

            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Invalid exam ID: " + examId
                            )
                    );


            if (subjectId == null) {

                exam.setSubject(null);

            } else {

                Subject subject =
                        subjectRepository.findById(subjectId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Invalid subject ID: "
                                                        + subjectId
                                        )
                                );

                exam.setSubject(subject);
            }


            examRepository.save(exam);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Subject assigned successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to assign subject."
            );
        }

        return "redirect:/admin/exams";
    }


    // ============================================================
    // MANAGE QUESTIONS
    // ============================================================

    @GetMapping("/questions")
    public String manageQuestions(Model model) {

        List<Question> questions =
                questionRepository.findAll();

        model.addAttribute(
                "questions",
                questions
        );

        model.addAttribute(
                "exams",
                examRepository.findAll()
        );

        return "admin/manage_questions";
    }


    // ============================================================
    // ADD QUESTION
    // ============================================================

    @GetMapping("/questions/new")
    public String newQuestion(Model model) {

        model.addAttribute(
                "question",
                new Question()
        );

        model.addAttribute(
                "exams",
                examRepository.findAll()
        );

        return "admin/question-form";
    }


    // ============================================================
    // SAVE QUESTION
    // ============================================================

    @PostMapping("/questions/save")
    public String saveQuestion(
            @ModelAttribute Question question,
            RedirectAttributes redirectAttributes) {

        try {

            questionRepository.save(question);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Question saved successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to save question."
            );
        }

        return "redirect:/admin/questions";
    }


    // ============================================================
    // EDIT QUESTION
    // ============================================================

    @GetMapping("/questions/edit/{id}")
    public String editQuestion(
            @PathVariable Long id,
            Model model) {

        Question question =
                questionRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid question ID: " + id
                                )
                        );

        model.addAttribute(
                "question",
                question
        );

        model.addAttribute(
                "exams",
                examRepository.findAll()
        );

        return "admin/question-form";
    }


    // ============================================================
    // DELETE QUESTION
    // ============================================================

    @GetMapping("/questions/delete/{id}")
    public String deleteQuestion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            questionRepository.deleteById(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Question deleted successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to delete question."
            );
        }

        return "redirect:/admin/questions";
    }


    // ============================================================
    // VIEW RESULTS
    // ============================================================

    @GetMapping("/results")
    public String viewResults(Model model) {

        List<ExamResult> results =
                examResultRepository.findAll();

        model.addAttribute(
                "results",
                results
        );

        return "admin/view_results";
    }


    // ============================================================
    // MANAGE STUDENTS
    // ============================================================

    @GetMapping("/students")
    public String manageStudents(Model model) {

        List<User> students =
                userRepository.findAll()
                        .stream()
                        .filter(user ->
                                "STUDENT".equalsIgnoreCase(
                                        user.getRole()
                                )
                        )
                        .collect(Collectors.toList());

        model.addAttribute(
                "students",
                students
        );

        return "admin/students";
    }


    // ============================================================
    // DELETE STUDENT
    // ============================================================

    @GetMapping("/students/delete/{id}")
    public String deleteStudent(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            Optional<User> user =
                    userRepository.findById(id);


            if (user.isPresent() &&
                    "STUDENT".equalsIgnoreCase(
                            user.get().getRole()
                    )) {

                userRepository.deleteById(id);

                redirectAttributes.addFlashAttribute(
                        "success",
                        "Student deleted successfully."
                );

            } else {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Student not found."
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to delete student."
            );
        }

        return "redirect:/admin/students";
    }


    // ============================================================
    // RESET STUDENT PASSWORD
    // ============================================================

    @PostMapping("/students/reset-password/{id}")
    public String resetStudentPassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes) {

        try {

            Optional<User> optionalUser =
                    userRepository.findById(id);


            if (optionalUser.isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Student not found."
                );

                return "redirect:/admin/students";
            }


            User student = optionalUser.get();


            if (!"STUDENT".equalsIgnoreCase(
                    student.getRole()
            )) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Invalid student account."
                );

                return "redirect:/admin/students";
            }


            student.setPassword(
                    passwordEncoder.encode(newPassword)
            );

            userRepository.save(student);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Student password reset successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to reset student password."
            );
        }

        return "redirect:/admin/students";
    }
}